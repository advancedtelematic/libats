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

package com.advancedtelematic.libats.codecs

import java.net.URI

import cats.syntax.either._
import io.circe._
import io.circe.{Decoder, Encoder}
import java.time.Instant
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.temporal.ChronoField
import java.util.UUID

import com.advancedtelematic.libats.data.DataType.HashMethod.HashMethod
import com.advancedtelematic.libats.data.DataType.{Checksum, HashMethod, Namespace}
import io.circe.generic.semiauto._

import scala.util.Try

trait CirceDateTime {
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

object CirceDateTime extends CirceDateTime

trait CirceUuid {
  implicit val javaUuidEncoder : Encoder[UUID] = Encoder[String].contramap(_.toString)
  implicit val javaUuidDecoder : Decoder[UUID] = Decoder[String].map(UUID.fromString)
}

object CirceUuid extends CirceUuid


trait CirceUri {
  implicit val urlEncoder: Encoder[URI] = Encoder.encodeString.contramap(_.toString)
  implicit val urlDecoder: Decoder[URI] = Decoder.decodeString.map(URI.create)
}

object CirceUri extends CirceUri

trait CirceAts {
  import CirceRefined._

  implicit val namespaceEncoder = Encoder.encodeString.contramap[Namespace](_.get)
  implicit val namespaceDecoder = Decoder.decodeString.map(Namespace.apply)

  implicit val hashMethodKeyEncoder: KeyEncoder[HashMethod] = KeyEncoder[String].contramap(_.toString)
  implicit val hashMethodKeyDecoder: KeyDecoder[HashMethod] = KeyDecoder.instance { value =>
    Try(HashMethod.withName(value)).toOption
  }

  implicit val hashMethodCodec: Codec[HashMethod] = Codec.codecForEnumeration(HashMethod)
  implicit val checkSumCodec: Codec[Checksum] = deriveCodec
}

object CirceAts extends CirceAts

trait CirceCodecs extends CirceDateTime
  with CirceUuid
  with CirceAnyVal
  with CirceRefined
  with CirceUri
  with CirceAts

object CirceCodecs extends CirceCodecs

@deprecated("use CirceCodecs instead", "0.0.1-109")
object AkkaCirce extends CirceCodecs
