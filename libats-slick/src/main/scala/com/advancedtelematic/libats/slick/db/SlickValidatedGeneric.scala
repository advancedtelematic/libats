package com.advancedtelematic.libats.slick.db

import cats.syntax.either._
import com.advancedtelematic.libats.data.ValidatedGeneric
import slick.jdbc.MySQLProfile.api._

import scala.reflect.ClassTag

object SlickValidatedGeneric {

  implicit def validatedGenericMapper[T, R](implicit gen: ValidatedGeneric[T, R], rtype: BaseColumnType[R], classTag: ClassTag[T]): BaseColumnType[T] =
    MappedColumnType.base[T, R](gen.to, gen.from(_).valueOr(throw _))

}