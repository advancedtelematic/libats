package com.advancedtelematic.libats.codecs

import com.advancedtelematic.libats.codecs.CirceValidatedString._
import com.advancedtelematic.libats.data.ValidatedStringConstructor
import com.advancedtelematic.libats.test.ValidatedStringTest
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.scalatest.{EitherValues, Matchers, PropSpec}

class CirceValidatedStringSpec extends PropSpec with Matchers with EitherValues {

  property("should encode a ValidatedString and decode it back") {
    val t = ValidatedStringConstructor[ValidatedStringTest]("abc").right.value
    val enc = implicitly[Encoder[ValidatedStringTest]]
    val dec = implicitly[Decoder[ValidatedStringTest]]
    dec(enc(t).hcursor).right.value shouldBe t
  }

  property("should fail to decode an invalid ValidatedString") {
    val t = Json.fromString("abXba")
    val dec = implicitly[Decoder[ValidatedStringTest]]
    dec(t.hcursor).left.value shouldBe a [DecodingFailure]
  }

}
