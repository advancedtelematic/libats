package com.advancedtelematic.libats.slick.db

import cats.syntax.either._
import com.advancedtelematic.libats.data.ValidatedGeneric
import slick.jdbc.MySQLProfile.api._

import scala.reflect.ClassTag

object SlickValidatedGeneric {

  private def validatedGenericMapper[T, R: BaseColumnType](implicit gen: ValidatedGeneric[T, R], classTag: ClassTag[T]): BaseColumnType[T] =
    MappedColumnType.base[T, R](gen.to, gen.from(_).valueOr(throw _))

  implicit def validatedStringMapper[T](implicit gen: ValidatedGeneric[T, String], classTag: ClassTag[T]): BaseColumnType[T] =
    validatedGenericMapper[T, String]

}