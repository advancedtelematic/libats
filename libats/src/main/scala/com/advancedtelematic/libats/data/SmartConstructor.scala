package com.advancedtelematic.libats.data

import scala.util.control.NoStackTrace

case class ValidationError(msg: String) extends Throwable with NoStackTrace

trait ValidatedStringConstructor[T] {
  def apply(s: String): Either[ValidationError, T]
  def value(t: T): String
}

object ValidatedStringConstructor {
  def apply[T](s: String)(implicit const: ValidatedStringConstructor[T]): Either[ValidationError, T] = const.apply(s)
}

abstract class ValidatedStringWrapper(val value: String) {

  require(value != null)

  def hashSeed: Int
  def canEqual(that: Any): Boolean

  override def toString: String = this.getClass.getSimpleName + "(" + value + ")"

  override def hashCode(): Int = hashSeed * value.hashCode

  override def equals(that: Any): Boolean = canEqual(that) && this.hashCode() == that.hashCode()
}