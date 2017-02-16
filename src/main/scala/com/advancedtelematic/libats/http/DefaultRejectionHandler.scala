/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._
import com.advancedtelematic.libats.codecs.{DeserializationException, RefinementError}
import io.circe.generic.auto._
import com.advancedtelematic.libats.codecs.AkkaCirce._
import de.heikoseeberger.akkahttpcirce.CirceSupport._

/**
  * When validation, JSON deserialisation fail or a duplicate entry
  * occures in the database, we complete the request by returning the
  * correct status code and JSON error message (see Errors.scala).
  */

object DefaultRejectionHandler {

  case class InvalidEntity(msg: String) extends Throwable(msg)

  case object DuplicateEntry extends Throwable("Entry already exists")

  def rejectionHandler : RejectionHandler = RejectionHandler.newBuilder().handle {
    case ValidationRejection(msg, _) =>
      complete( StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg) )
  }.handle{
    case MalformedRequestContentRejection(_, RefinementError(_, msg)) =>
      complete(StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg))
  }.handle{
    case MalformedRequestContentRejection(_, DeserializationException(RefinementError(_, msg))) =>
      complete(StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg))
  }.result().withFallback(RejectionHandler.default)
}
