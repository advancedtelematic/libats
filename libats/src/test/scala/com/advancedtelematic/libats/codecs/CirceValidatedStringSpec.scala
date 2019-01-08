package com.advancedtelematic.libats.codecs

import com.advancedtelematic.libats.codecs.CirceValidatedString._
import com.advancedtelematic.libats.data.{ValidatedString, ValidatedStringConstructor}
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.scalatest.{EitherValues, Matchers, PropSpec}

class CirceValidatedStringSpec extends PropSpec with Matchers with EitherValues {

  property("should encode a SmartStringConstructor and decode it back") {
    val t = ValidatedStringConstructor[ValidatedString]("abc").right.value
    val enc = implicitly[Encoder[ValidatedString]]
    val dec = implicitly[Decoder[ValidatedString]]
    dec(enc(t).hcursor).right.value shouldBe t
  }

  property("should fail to decode an invalid SmartStringConstructor") {
    val t = Json.fromString("abXba")
    val dec = implicitly[Decoder[ValidatedString]]
    dec(t.hcursor).left.value shouldBe a [DecodingFailure]
  }

}
