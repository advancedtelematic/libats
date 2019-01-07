package com.advancedtelematic.libats.slick.db

import cats.syntax.either._
import com.advancedtelematic.libats.data.SmartStringConstructor
import slick.jdbc.MySQLProfile.api._

import scala.reflect.ClassTag

object SlickSmartConstructor {

  implicit def smartConstructorStringMapping[T](implicit cons: SmartStringConstructor[T], classTag: ClassTag[T]): BaseColumnType[T] =
    MappedColumnType.base[T, String](
      (t : T) => cons.value(t),
      (s: String) => cons.apply(s).valueOr(throw _)
    )
}
