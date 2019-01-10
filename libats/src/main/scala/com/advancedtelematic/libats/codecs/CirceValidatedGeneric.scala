package com.advancedtelematic.libats.codecs

import cats.syntax.either._
import com.advancedtelematic.libats.data.ValidatedGeneric
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}

object CirceValidatedGeneric {

  implicit def validatedGenericEncoder[T, R](implicit gen: ValidatedGeneric[T, R], encodeR: Encoder[R]): Encoder[T] =
    encodeR.contramap(gen.to)

  implicit def validatedGenericDecoder[T, R](implicit gen: ValidatedGeneric[T, R], decodeR: Decoder[R]): Decoder[T] =
    decodeR.emap(gen.from(_).left.map(_.msg))

  implicit def validatedGenericKeyEncoder[T, R](implicit gen: ValidatedGeneric[T, R], encodeR: KeyEncoder[R]): KeyEncoder[T] =
    encodeR.contramap(gen.to)

  implicit def validatedGenericKeyDecoder[T, R](implicit gen: ValidatedGeneric[T, R], decodeR: KeyDecoder[R]): KeyDecoder[T] =
    decodeR.map(gen.from(_).valueOr(throw _))

}