package com.advancedtelematic.libats.slick.db

import java.util.UUID

import com.advancedtelematic.libats.data.UUIDKey.UUIDKey
import shapeless.{Generic, HNil}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import scala.reflect.ClassTag
import slick.jdbc.MySQLProfile.api._
import shapeless._

object SlickUUIDKey {

  implicit def dbMapping[T <: UUIDKey]
  (implicit gen: Generic.Aux[T, UUID :: HNil], ct: ClassTag[T]): JdbcType[T] with BaseTypedType[T] =
    MappedColumnType.base[T, String](_.uuid.toString, (s: String) => gen.from(UUID.fromString(s) :: HNil))
}
