/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.codecs

import io.circe.{Decoder, Encoder}

trait CirceEnum extends Enumeration {
  implicit val encode: Encoder[Value] = Encoder[String].contramap(_.toString)
  implicit val decode: Decoder[Value] = Decoder[String].map(this.withName)
}
