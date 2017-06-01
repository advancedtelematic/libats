package com.advancedtelematic.libats.codecs

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import io.circe.{Decoder, Encoder}

trait CirceRefined {
  implicit def refinedEncoder[T, P](implicit encoder: Encoder[T]): Encoder[Refined[T, P]] =
    encoder.contramap(_.value)

  implicit def refinedDecoder[T, P](implicit decoder: Decoder[T], p: Validate.Plain[T, P]): Decoder[Refined[T, P]] =
    decoder.map(t =>
      refineV[P](t) match {
        case Left(e)  =>
          throw DeserializationException(RefinementError(t, e))
        case Right(r) => r
      })
}

object CirceRefined extends CirceRefined
