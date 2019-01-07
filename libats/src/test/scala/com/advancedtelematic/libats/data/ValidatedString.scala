package com.advancedtelematic.libats.data

class ValidatedString private(val value: String) {
  require(value != null)

  override def hashCode(): Int = 23 * value.hashCode

  override def equals(that: Any): Boolean = that match {
    case that: ValidatedString => this.hashCode() == that.hashCode()
    case _ => false
  }

  override def toString: String = value
}

object ValidatedString {

  implicit val constructor: SmartStringConstructor[ValidatedString] = new SmartStringConstructor[ValidatedString] {
    override def apply(s: String): Either[ValidationError, ValidatedString] =
      if (s contains 'X') Left(ValidationError("The value can't contain 'X'."))
      else                Right(new ValidatedString(s))

    override def value(v: ValidatedString): String = v.value
  }

}