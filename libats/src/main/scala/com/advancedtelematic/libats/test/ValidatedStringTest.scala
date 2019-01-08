package com.advancedtelematic.libats.test

import com.advancedtelematic.libats.data.{ValidatedStringConstructor, ValidatedStringWrapper, ValidationError}

class ValidatedStringTest private(value: String) extends ValidatedStringWrapper(value) {
  override def hashSeed: Int = 23
  override def equals(that: Any): Boolean =
    that.isInstanceOf[ValidatedStringTest] && that.asInstanceOf[ValidatedStringTest].value == value
}

object ValidatedStringTest {

  implicit val constructor: ValidatedStringConstructor[ValidatedStringTest] = new ValidatedStringConstructor[ValidatedStringTest] {

    override def apply(s: String): Either[ValidationError, ValidatedStringTest] =
      if (s contains 'X') Left(ValidationError("The value can't contain 'X'."))
      else                Right(new ValidatedStringTest(s))

    override def value(v: ValidatedStringTest): String = v.value
  }

}