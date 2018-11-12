package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.data.Urn.{CorrelationId}
import slick.jdbc.MySQLProfile.api._

object SlickUrnMapper {
  implicit val correlationIdMapper = MappedColumnType.base[CorrelationId, String](
    _.toString,
    CorrelationId.fromString(_)
  )
}

