/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package com.advancedtelematic.libats.slick.codecs

import slick.jdbc.MySQLProfile.api._

object SlickEnumMapper {
  def enumMapper[E <: Enumeration](enum: E) = {
    MappedColumnType.base[E#Value, String](_.toString, (s: String) => enum.withName(s))
  }
}

@deprecated("Consider using a sealed trait, enumeratum, or use SlickEnumMapper", since = "v0.0.1-103")
trait SlickEnum {
  self: Enumeration =>

  implicit val enumMapper =
    MappedColumnType.base[Value, String](_.toString, this.withName)
}
