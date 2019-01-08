package com.advancedtelematic.libats.data

class EcuIdentifier private(value: String) extends ValidatedStringWrapper(value) {
  override def hashSeed: Int = 13
  override def canEqual(that: Any): Boolean = that.isInstanceOf[EcuIdentifier]
}

object EcuIdentifier {

  implicit val constructor: ValidatedStringConstructor[EcuIdentifier] = new ValidatedStringConstructor[EcuIdentifier] {
    override def apply(s: String): Either[ValidationError, EcuIdentifier] =
      if (s.length < 0 || s.length > 64) Left(ValidationError("At least one and at most 64 characters are allowed for the ECU identifier."))
      else                               Right(new EcuIdentifier(s))

    override def value(e: EcuIdentifier): String = e.value
  }
}
