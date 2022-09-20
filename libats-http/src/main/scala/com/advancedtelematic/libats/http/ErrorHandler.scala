/*
 * Copyright (C) 2016 HERE Global B.V.
 *
 * Licensed under the Mozilla Public License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.mozilla.org/en-US/MPL/2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: MPL-2.0
 * License-Filename: LICENSE
 */

package com.advancedtelematic.libats.http

import java.util.UUID

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes, Uri}
import akka.http.scaladsl.server.{Directives, ExceptionHandler, _}
import cats.Show
import com.advancedtelematic.libats.data.{ErrorCode, ErrorCodes, ErrorRepresentation}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.syntax._
import com.advancedtelematic.libats.codecs.CirceUuid._
import io.circe.{DecodingFailure, Json}
import cats.syntax.option._
import cats.syntax.show
import shapeless.{Generic, HNil}

import scala.reflect.ClassTag
import scala.util.control.NoStackTrace
import scala.language.existentials

object Errors {
  import Directives._
  import ErrorRepresentation._

  abstract class Error(val code: ErrorCode,
                       val responseCode: StatusCode,
                       val msg: String,
                       val cause: Option[Throwable] = None,
                       val errorId: UUID = UUID.randomUUID()) extends Exception(msg, cause.orNull) with NoStackTrace

  case class JsonError(code: ErrorCode,
                       responseCode: StatusCode,
                       json: Json,
                       msg: String,
                       errorId: UUID = UUID.randomUUID()) extends Exception(msg) with NoStackTrace

  case class RawError(code: ErrorCode,
                      responseCode: StatusCode,
                      desc: String,
                      errorId: UUID = UUID.randomUUID()) extends Exception(desc) with NoStackTrace

  case class RemoteServiceError(msg: String,
                                status: StatusCode,
                                description: Json = Json.Null,
                                causeCode: ErrorCode = ErrorCodes.RemoteServiceError,
                                cause: Option[ErrorRepresentation] = None,
                                errorId: UUID = UUID.randomUUID()
                               ) extends Exception(s"Remote Service Error: $msg") with NoStackTrace

  case class MissingEntityId[T](id: T)(implicit ct: ClassTag[T], show: Show[T]) extends
    Error(ErrorCodes.MissingEntity, StatusCodes.NotFound, s"Missing entity: ${ct.runtimeClass.getSimpleName} ${show.show(id)}")

  case class MissingEntity[T]()(implicit ct: ClassTag[T]) extends
    Error(ErrorCodes.MissingEntity, StatusCodes.NotFound, s"Missing entity: ${ct.runtimeClass.getSimpleName}")

  case class EntityAlreadyExists[T]()(implicit ct: ClassTag[T]) extends
    Error(ErrorCodes.ConflictingEntity, StatusCodes.Conflict, s"Entity already exists: ${ct.runtimeClass.getSimpleName}")

  val TooManyElements = RawError(ErrorCodes.TooManyElements, StatusCodes.InternalServerError, "Too many elements found")

  type PF = PartialFunction[Throwable, (StatusCode, ErrorRepresentation)]

  private val onRawError: PF = {
    case RawError(code, statusCode, desc, uuid) =>
      statusCode -> ErrorRepresentation(code, desc, None, uuid.some)
  }

  private[http] val onDecodingError: PF = {
    case df: DecodingFailure =>
      StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, df.getMessage())
  }

  private val onJsonError: PF = {
    case JsonError(code, statusCode, json, description, errorId) =>
      statusCode -> ErrorRepresentation(code, description, json.some, errorId.some)
  }

  private val onRemoteServiceError: PF = {
    case RemoteServiceError(msg, _, _, code, cause, errorId) =>
      StatusCodes.BadGateway -> ErrorRepresentation(code, msg, cause.map(_.asJson), errorId.some)
  }

  private val onError: PF = {
    case e : Error =>
      e.responseCode -> ErrorRepresentation(e.code, e.msg, e.cause.map(_.getMessage.asJson), e.errorId.some)
  }

  private val onIntegrityViolationError: PF = {
    case err: java.sql.SQLIntegrityConstraintViolationException if err.getErrorCode == 1062 =>
      StatusCodes.Conflict -> ErrorRepresentation(ErrorCodes.ConflictingEntity, "Entry already exists")
  }

  // Add more handlers here, or use RawError
  private val toErrorRepresentation: PartialFunction[Throwable, (StatusCode, ErrorRepresentation)] =
    Seq(
      onDecodingError,
      onJsonError,
      onError,
      onRemoteServiceError,
      onIntegrityViolationError,
      onRawError
    ).foldLeft(PartialFunction.empty[Throwable, (StatusCode, ErrorRepresentation)])(_ orElse _)

  val logAndHandleErrors: PartialFunction[Throwable, Route] =
    toErrorRepresentation andThen {
      case (status, errorRepr) =>
        extractLog { log =>
          log.error(s"An error occurred. ErrorId: ${errorRepr.errorId} ${errorRepr.asJson.noSpaces}")
          complete(status -> errorRepr)
        }
    }
}

object ErrorHandler {
  import Directives._

  def logError(log: LoggingAdapter, uri: Uri, error: Throwable): UUID = {
    val id = UUID.randomUUID()
    log.error(error, s"Request error $id ($uri)")
    id
  }

  def errorRepr(id: UUID, error: Throwable): ErrorRepresentation = {
    ErrorRepresentation(
      ErrorCodes.InternalServerError,
      description = Option(error.getMessage).getOrElse("an error occurred"),
      errorId = id.some,
    )
  }

  private def defaultHandler: ExceptionHandler =
    Errors.logAndHandleErrors orElse ExceptionHandler {
      case e: Throwable =>
        (extractLog & extractUri) { (log, uri) =>
          val errorId = logError(log, uri, e)
          val entity = errorRepr(errorId, e)
          complete(InternalServerError -> entity.asJson)
        }
    }

  val handleErrors: Directive0 = handleExceptions(defaultHandler)
}
