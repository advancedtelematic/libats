/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.codecs

import akka.http.scaladsl.model.Uri
import cats.syntax.either._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import io.circe._
import io.circe.{Decoder, Encoder}
import java.time.Instant
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.temporal.ChronoField
import java.util.UUID

trait AkkaCirce {

  implicit def refinedEncoder[T, P](implicit encoder: Encoder[T]): Encoder[Refined[T, P]] =
    encoder.contramap(_.get)

  implicit def refinedDecoder[T, P](implicit decoder: Decoder[T], p: Validate.Plain[T, P]): Decoder[Refined[T, P]] =
    decoder.map(t =>
      refineV[P](t) match {
        case Left(e)  =>
          throw DeserializationException(RefinementError(t, e))
        case Right(r) => r
      })

  implicit val uriEncoder : Encoder[Uri] = Encoder.instance { uri =>
    Json.obj(("uri", Json.fromString(uri.toString())))
  }

  implicit val uriDecoder : Decoder[Uri] = Decoder.instance { c =>
    c.focus.flatMap(_.asObject) match {
      case None      => Either.left(DecodingFailure("Uri", c.history))
      case Some(obj) => obj.toMap.get("uri").flatMap(_.asString) match {
        case None      => Either.left(DecodingFailure("Uri", c.history))
        case Some(uri) => Either.right(Uri(uri))
      }
    }
  }

  implicit val javaUuidEncoder : Encoder[UUID] = Encoder[String].contramap(_.toString)
  implicit val javaUuidDecoder : Decoder[UUID] = Decoder[String].map(UUID.fromString)

  implicit val dateTimeEncoder : Encoder[Instant] = Encoder.instance[Instant] { instant =>
    Json.fromString {
      instant
        .`with`(ChronoField.MILLI_OF_SECOND, 0)
        .`with`(ChronoField.NANO_OF_SECOND, 0)
        .toString
    }
  }

  implicit val dateTimeDecoder : Decoder[Instant] = Decoder.instance { c =>
    c.focus.flatMap(_.asString) match {
      case None       => Either.left(DecodingFailure("DataTime", c.history))
      case Some(date) =>
        try {
          val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
          val nst = Instant.from(fmt.parse(date))
          Either.right(nst)
        } catch {
          case t: DateTimeParseException =>
            Either.left(DecodingFailure("DateTime", c.history))
        }
    }
  }
}

object AkkaCirce extends AkkaCirce

