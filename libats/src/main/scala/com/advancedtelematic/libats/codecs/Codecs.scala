package com.advancedtelematic.libats.codecs

import com.advancedtelematic.libats.data.Namespace
import io.circe.{Decoder, Encoder}

object Codecs {
  implicit val namespaceEncoder = Encoder.encodeString.contramap[Namespace](_.get)
  implicit val namespaceDecoder = Decoder.decodeString.map(Namespace.apply)
}
