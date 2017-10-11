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

import scala.reflect.ClassTag
import scala.util.control.NoStackTrace

object Errors {
  import Directives._
  import ErrorRepresentation._

  import scala.language.existentials

  trait Error[T] extends NoStackTrace {
    implicit val ct: ClassTag[T]
    def name = ct.runtimeClass.getSimpleName
  }

  case class MissingEntity[T]()(implicit val ct: ClassTag[T]) extends Throwable(s"Missing entity: ${ct.runtimeClass.getSimpleName}") with Error[T]
  case class EntityAlreadyExists[T]()(implicit val ct: ClassTag[T]) extends Throwable(s"Entity already exists: ${ct.runtimeClass.getSimpleName}") with Error[T]

  case class RawError(code: ErrorCode,
                      responseCode: StatusCode,
                      desc: String) extends Exception(desc) with NoStackTrace

  val TooManyElements = RawError(ErrorCodes.TooManyElements, StatusCodes.InternalServerError, "Too many elements found")

  private val onRawError: PF = {
    case RawError(code, statusCode, desc) =>
      complete(statusCode -> ErrorRepresentation(code, desc))
  }

  private val onMissingEntity: PF = {
    case me @ MissingEntity() =>
      complete(StatusCodes.NotFound ->
        ErrorRepresentation(ErrorCodes.MissingEntity, s"${me.name} not found"))
  }

  private val onConflictingEntity: PF = {
    case eae @ EntityAlreadyExists() =>
      complete(StatusCodes.Conflict ->
        ErrorRepresentation(ErrorCodes.ConflictingEntity, s"${eae.name} already exists"))
  }

  private val onIntegrityViolationError: PF = {
    case err: java.sql.SQLIntegrityConstraintViolationException if err.getErrorCode == 1062 =>
      complete(StatusCodes.Conflict ->
        ErrorRepresentation(ErrorCodes.ConflictingEntity, "Entry already exists"))
  }

  // Add more handlers here, or use RawError
  val handleAllErrors =
    Seq(
      onMissingEntity,
      onConflictingEntity,
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
