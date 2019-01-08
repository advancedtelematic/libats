package com.advancedtelematic.libats.data

class ValidatedString private(value: String) extends ValidatedStringWrapper(value){
  override def hashSeed: Int = 23
  override def canEqual(that: Any): Boolean = that.isInstanceOf[ValidatedString]
}

object ValidatedString {

  implicit val constructor: ValidatedStringConstructor[ValidatedString] = new ValidatedStringConstructor[ValidatedString] {

    override def apply(s: String): Either[ValidationError, ValidatedString] =
      if (s contains 'X') Left(ValidationError("The value can't contain 'X'."))
      else                Right(new ValidatedString(s))

    override def value(v: ValidatedString): String = v.value
  }

}