package com.advancedtelematic.libats.data

import scala.util.control.NoStackTrace

case class ValidationError(msg: String) extends Throwable with NoStackTrace

trait SmartStringConstructor[T] {
  def apply(s: String): Either[ValidationError, T]
  def value(t: T): String
}

object SmartStringConstructor {
  def apply[T](s: String)(implicit const: SmartStringConstructor[T]): Either[ValidationError, T] = const.apply(s)
}