package com.advancedtelematic.libats.data

trait ValidatedGeneric[T, Repr] {
  def to(t: T): Repr
  def from(r: Repr): Either[ValidationError, T]
}

object ValidatedGeneric {
  def apply[T](t: => T, b: Boolean, msg: String): Either[ValidationError, T] =
    if (b) Right(t) else Left(ValidationError(msg))
}