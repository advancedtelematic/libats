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

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._
import com.advancedtelematic.libats.codecs.AkkaCirce._
import com.advancedtelematic.libats.codecs.{DeserializationException, RefinementError}
import com.advancedtelematic.libats.data.{ErrorCodes, ErrorRepresentation}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.DecodingFailure
import io.circe.generic.auto._

/**
  * When validation, JSON deserialisation fail or a duplicate entry
  * occurs in the database, we complete the request by returning the
  * correct status code and JSON error message (see Errors.scala).
  */

object DefaultRejectionHandler {
  def rejectionHandler : RejectionHandler = RejectionHandler.newBuilder().handle {
    case ValidationRejection(msg, _) =>
      complete( StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg) )
  }.handle{
    case MalformedRequestContentRejection(_, RefinementError(_, msg)) =>
      complete(StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg))
  }.handle{
    case MalformedRequestContentRejection(_, DeserializationException(RefinementError(_, msg))) =>
      complete(StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg))
  }.handle {
    case MalformedRequestContentRejection(_, df@DecodingFailure(_, _)) =>
      complete(StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, df.getMessage))
  }.handle {
    case MalformedQueryParamRejection(name, _, _) ⇒
      complete(StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, "The query parameter '" + name + "' was malformed"))
  }.result().withFallback(RejectionHandler.default)
}
