package com.advancedtelematic.libats.slick.db

import cats.syntax.either._
import com.advancedtelematic.libats.data.DataType.CorrelationId
import slick.jdbc.MySQLProfile.api._

object SlickUrnMapper {
  implicit val correlationIdMapper = MappedColumnType.base[CorrelationId, String](
    _.toString,
    CorrelationId.fromString(_).valueOr(s => throw new IllegalArgumentException(s))
  )
}

