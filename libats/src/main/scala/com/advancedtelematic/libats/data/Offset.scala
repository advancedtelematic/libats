package com.advancedtelematic.libats.data

import io.circe.{Decoder, Encoder}

case class Offset(value: Long)

object Offset {
  implicit val offsetEncoder: Encoder[Offset] = Encoder.encodeLong.contramap(_.value)
  implicit val offsetDecoder: Decoder[Offset] = Decoder.decodeLong.map(Offset(_))
}