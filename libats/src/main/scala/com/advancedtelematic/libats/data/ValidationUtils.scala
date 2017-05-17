package com.advancedtelematic.libats.data

import eu.timepit.refined.api.Validate

object ValidationUtils {
  def validHex(length: Long, str: String): Boolean = {
    str.length == length && str.forall(h => ('0' to '9').contains(h) || ('a' to 'f').contains(h))
  }

  def validHexValidation[T](v: T, length: Int): Validate.Plain[String, T] =
    Validate.fromPredicate(
      hash => validHex(length, hash),
      hash => s"$hash is not a $length hex string",
      v
    )

  def validInBetween[T](min: Long, max: Long, proof: T): Validate.Plain[String, T] =
    Validate.fromPredicate(
      str => str.length >= min && str.length <= max,
      str => s"$str is not between $min and $max chars long",
      proof
    )
}
