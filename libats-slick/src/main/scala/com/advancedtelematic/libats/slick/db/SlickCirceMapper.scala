package com.advancedtelematic.libats.slick.db

import cats.syntax.either._
import io.circe.Json
import io.circe.syntax._
import slick.jdbc.MySQLProfile.api._

import scala.reflect.ClassTag

object SlickCirceMapper {
  import io.circe.parser.decode
  import io.circe.{Decoder, Encoder}

  /*
   It's easy to misuse this it's implicit, if there is an encoder in scope
   for an object, it will encode it as json instead of it's default encoding
   so we define mapper values for specific types
   */
  def circeMapper[T : Encoder : Decoder : ClassTag] = MappedColumnType.base[T, String](
    _.asJson.noSpaces,
    str => decode(str).valueOr(throw _)
  )

  implicit val jsonMapper = circeMapper[Json]
}
