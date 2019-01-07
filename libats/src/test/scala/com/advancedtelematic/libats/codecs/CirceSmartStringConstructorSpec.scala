package com.advancedtelematic.libats.codecs

import com.advancedtelematic.libats.codecs.CirceSmartConstructor._
import com.advancedtelematic.libats.data.{SmartStringConstructor, ValidatedString}
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.scalatest.{EitherValues, Matchers, PropSpec}

class CirceSmartStringConstructorSpec extends PropSpec with Matchers with EitherValues {

  property("should encode a SmartConstructor and decode it back") {
    val t = SmartStringConstructor[ValidatedString]("abc")
    val enc = implicitly[Encoder[ValidatedString]]
    val dec = implicitly[Decoder[ValidatedString]]
    dec.apply(enc.apply(t.right.value).hcursor) shouldBe t
  }

  property("should fail to decode an invalid SmartConstructor") {
    val t = Json.fromString("abXba")
    val dec = implicitly[Decoder[ValidatedString]]
    dec.apply(t.hcursor).left.value shouldBe a [DecodingFailure]
  }

}
