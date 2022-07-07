package com.advancedtelematic.libats.data

import io.circe.{Decoder, Encoder}

case class Limit(value: Long) {
  def min(that: Limit): Limit = if (this.value < that.value) this else that
}

object Limit {
  implicit val limitEncoder: Encoder[Limit] = Encoder.encodeLong.contramap(_.value)
  implicit val limitDecoder: Decoder[Limit] = Decoder.decodeLong.map(Limit(_))
}
