/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package com.advancedtelematic.libats.http

import java.util.UUID

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes, Uri}
import akka.http.scaladsl.server.ExceptionHandler.PF
import akka.http.scaladsl.server.{Directives, ExceptionHandler, _}
import com.advancedtelematic.libats.data.{ErrorCode, ErrorCodes, ErrorRepresentation}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.syntax._
import com.advancedtelematic.libats.codecs.CirceUuid._
import io.circe.Json
import cats.syntax.option._

import scala.reflect.ClassTag
import scala.util.control.NoStackTrace
import scala.language.existentials

object Errors {
  import Directives._
  import ErrorRepresentation._

  abstract class Error[T](val code: ErrorCode,
                          val responseCode: StatusCode,
                          val msg: String,
                          val cause: Option[Throwable] = None,
                          val errorId: UUID = UUID.randomUUID()) extends Throwable(msg, cause.orNull) with NoStackTrace

  case class JsonError(code: ErrorCode,
                       responseCode: StatusCode,
                       json: Json,
                       msg: String,
                       errorId: UUID = UUID.randomUUID()) extends Throwable(msg) with NoStackTrace

  case class RawError(code: ErrorCode,
                      responseCode: StatusCode,
                      desc: String,
                      errorId: UUID = UUID.randomUUID()) extends Exception(desc) with NoStackTrace

  case class MissingEntity[T]()(implicit ct: ClassTag[T]) extends
    Error[T](ErrorCodes.MissingEntity, StatusCodes.NotFound, s"Missing entity: ${ct.runtimeClass.getSimpleName}")

  case class EntityAlreadyExists[T]()(implicit ct: ClassTag[T]) extends
    Error[T](ErrorCodes.ConflictingEntity, StatusCodes.Conflict, s"Entity already exists: ${ct.runtimeClass.getSimpleName}")

  def RemoteServiceError(msg: String, cause: Option[Json] = None, errorId: UUID = UUID.randomUUID()) = {
    val causeJson = cause.getOrElse(Json.obj())
    JsonError(ErrorCodes.RemoteServiceError, StatusCodes.BadGateway, causeJson, msg, errorId)
  }

  val TooManyElements = RawError(ErrorCodes.TooManyElements, StatusCodes.InternalServerError, "Too many elements found")

  private val onRawError: PF = {
    case RawError(code, statusCode, desc, uuid) =>
      complete(statusCode -> ErrorRepresentation(code, desc, None, uuid.some))
  }

  private val onJsonError: PF = {
    case JsonError(code, statusCode, json, description, errorId) =>
      complete(statusCode -> ErrorRepresentation(code, description, json.some, errorId.some))
  }

  private val onError: PF = {
    case e : Error[_] =>
      complete(e.responseCode -> ErrorRepresentation(e.code, e.msg, e.cause.map(_.getMessage.asJson), e.errorId.some))
  }

  private val onIntegrityViolationError: PF = {
    case err: java.sql.SQLIntegrityConstraintViolationException if err.getErrorCode == 1062 =>
      complete(StatusCodes.Conflict ->
        ErrorRepresentation(ErrorCodes.ConflictingEntity, "Entry already exists"))
  }

  // Add more handlers here, or use RawError
  val handleAllErrors =
    Seq(
      onJsonError,
      onError,
      onIntegrityViolationError,
      onRawError
    ).foldLeft(PartialFunction.empty[Throwable, Route])(_ orElse _)
}

object ErrorHandler {
  import Directives._

  private lazy val isProd =
    Option(System.getenv("DEPLOY_ENV")).exists(env => env == "production" || env.isEmpty)

  def logError(log: LoggingAdapter, uri: Uri, error: Throwable): UUID = {
    val id = UUID.randomUUID()
    log.error(error, s"Request error $id ($uri)")
    id
  }

  def errorRepr(id: UUID, error: Throwable): Json = {
    if(isProd)
      Json.obj(
        "error_id" -> id.asJson,
        "description" -> "an error occurred".asJson
      )
    else
      Json.obj(
        "error_id" -> id.asJson,
        "description" ->  Json.fromString(Option(error.getMessage).getOrElse("<empty error description>"))
      )
  }

  private def defaultHandler(): ExceptionHandler =
    Errors.handleAllErrors orElse ExceptionHandler {
      case e: Throwable =>
        (extractLog & extractUri) { (log, uri) =>
          val errorId = logError(log, uri, e)
          val entity = errorRepr(errorId, e)
          complete(HttpResponse(InternalServerError, entity = entity.noSpaces))
        }
    }

  val handleErrors: Directive0 = handleExceptions(defaultHandler())
}
