/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package com.advancedtelematic.libats.http

import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.ExceptionHandler.PF
import akka.http.scaladsl.server.{Directives, ExceptionHandler, _}
import io.circe.Json
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

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
  import Json.obj

  private def defaultHandler(): ExceptionHandler =
    Errors.handleAllErrors orElse ExceptionHandler {
      case e: Throwable =>
        (extractLog & extractUri) { (log, uri) =>
          log.error(e, s"Request to $uri error")
          val entity = obj("error" -> Json.fromString(Option(e.getMessage).getOrElse("")))
          complete(HttpResponse(InternalServerError, entity = entity.toString()))
        }
    }

  val handleErrors: Directive0 = handleExceptions(defaultHandler())
}
