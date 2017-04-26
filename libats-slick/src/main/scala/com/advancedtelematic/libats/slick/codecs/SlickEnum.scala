/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package com.advancedtelematic.libats.slick.codecs

import slick.jdbc.MySQLProfile.api._

trait SlickEnum {
  self: Enumeration =>

  implicit val enumMapper =
    MappedColumnType.base[Value, String](_.toString, this.withName)
}
