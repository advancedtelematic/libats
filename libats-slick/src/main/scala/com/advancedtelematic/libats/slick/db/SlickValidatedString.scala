package com.advancedtelematic.libats.slick.db

import cats.syntax.either._
import com.advancedtelematic.libats.data.ValidatedStringConstructor
import slick.jdbc.MySQLProfile.api._

import scala.reflect.ClassTag

object SlickValidatedString {

  implicit def validatedStringMapping[T](implicit cons: ValidatedStringConstructor[T], classTag: ClassTag[T]): BaseColumnType[T] =
    MappedColumnType.base[T, String](
      (t : T) => cons.value(t),
      (s: String) => cons.apply(s).valueOr(throw _)
    )
}
