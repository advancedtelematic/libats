package com.advancedtelematic.libats.codecs

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import io.circe.{Decoder, Encoder}

import scala.util.{Failure, Success}

trait CirceRefined {
  implicit def refinedEncoder[T, P](implicit encoder: Encoder[T]): Encoder[Refined[T, P]] =
    encoder.contramap(_.value)

  implicit def refinedDecoder[T, P](implicit decoder: Decoder[T], p: Validate.Plain[T, P]): Decoder[Refined[T, P]] =
    decoder.emapTry { t =>
      refineV[P](t) match {
        case Left(e) => Failure(DeserializationException(RefinementError(t, e)))
        case Right(r) => Success(r)
      }
    }
}

object CirceRefined extends CirceRefined
