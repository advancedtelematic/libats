package com.advancedtelematic.libats.data

class EcuIdentifier private(val value: String) {
  require(value != null)

  override def hashCode(): Int = 13 * value.hashCode

  override def equals(that: Any): Boolean = that match {
    case that: EcuIdentifier => this.hashCode() == that.hashCode()
    case _ => false
  }

  override def toString: String = value
}

object EcuIdentifier {

  implicit val constructor: SmartStringConstructor[EcuIdentifier] = new SmartStringConstructor[EcuIdentifier] {
    override def apply(s: String): Either[ValidationError, EcuIdentifier] =
      if (s.length < 0 || s.length > 64) Left(ValidationError("At least one and at most 64 characters are allowed for the ECU identifier."))
      else                               Right(new EcuIdentifier(s))

    override def value(e: EcuIdentifier): String = e.value
  }
}
