package com.advancedtelematic.libats.data

trait ValidatedGeneric[T, Repr] {
  def to(t: T): Repr
  def from(r: Repr): Either[ValidationError, T]
}