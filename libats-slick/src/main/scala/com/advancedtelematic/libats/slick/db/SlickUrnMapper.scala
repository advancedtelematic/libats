package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.data.DataType.CorrelationId
import slick.jdbc.MySQLProfile.api._
import io.circe.parser.decode
import io.circe.syntax._

object SlickUrnMapper {
  implicit val correlationIdMapper = MappedColumnType.base[CorrelationId, String](
    _.toString,
    CorrelationId.fromString(_) match {
      case Right(x) => x
      case Left(err) => throw new Exception(err)
    }
  )
}

