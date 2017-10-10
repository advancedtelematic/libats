/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.codecs

import io.circe.{Decoder, Encoder}

@deprecated("Consider using a sealed trait, enumeratum or define custom codecs using Encoder.enumEncoder and Decoder.enumDecoder")
trait CirceEnum extends Enumeration {
  implicit val encode: Encoder[Value] = Encoder.enumEncoder(this)
  implicit val decode: Decoder[Value] = Decoder.enumDecoder(this)
}
