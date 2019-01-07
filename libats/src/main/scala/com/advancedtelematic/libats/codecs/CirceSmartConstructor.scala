package com.advancedtelematic.libats.codecs

import cats.syntax.either._
import com.advancedtelematic.libats.data.SmartStringConstructor
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}

trait CirceSmartConstructor {

  implicit def smartConstructorStringEncoder[T](implicit cons: SmartStringConstructor[T]): Encoder[T] =
    Encoder.encodeString.contramap(cons.value)

  implicit def smartConstructorStringDecoder[T](implicit cons: SmartStringConstructor[T]): Decoder[T] =
    Decoder.decodeString.emap(s => cons.apply(s).leftMap(_.msg))

  implicit def keyEncoderEcuIdentifier[T](implicit cons: SmartStringConstructor[T]): KeyEncoder[T] =
    KeyEncoder[String].contramap(cons.value)

  implicit def keyDecoderEcuIdentifier[T](implicit cons: SmartStringConstructor[T]): KeyDecoder[T] =
    KeyDecoder.instance(cons.apply(_).toOption)
}

object CirceSmartConstructor extends CirceSmartConstructor
