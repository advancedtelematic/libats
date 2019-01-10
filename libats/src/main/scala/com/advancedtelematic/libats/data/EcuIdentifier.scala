package com.advancedtelematic.libats.data

import com.advancedtelematic.libats.codecs.CirceValidatedGeneric
import io.circe.{Decoder, Encoder}

final case class EcuIdentifier private[EcuIdentifier](value: String) extends AnyVal

object EcuIdentifier {

  implicit val validatedEcuIdentifier = new ValidatedGeneric[EcuIdentifier, String] {
    override def to(ecuId: EcuIdentifier): String = ecuId.value
    override def from(s: String): Either[ValidationError, EcuIdentifier] = apply(s)
  }

  def apply(s: String): Either[ValidationError, EcuIdentifier] =
    if (s.length < 0 || s.length > 64) Left(ValidationError("An ECU identifier must have at least 1 and at most 64 characters."))
    else Right(new EcuIdentifier(s))

  implicit val ecuIdentifierEncoder: Encoder[EcuIdentifier] = CirceValidatedGeneric.validatedGenericEncoder
  implicit val ecuIdentifierDecoder: Decoder[EcuIdentifier] = CirceValidatedGeneric.validatedGenericDecoder

}
