package com.advancedtelematic.libats.data

import com.advancedtelematic.libats.test.ValidatedStringTest
import org.scalatest.{EitherValues, Matchers, PropSpec}

class ValidatedStringSpec extends PropSpec with Matchers with EitherValues {

  property("creating a valid ValidatedString should succeed") {
    ValidatedStringConstructor[ValidatedStringTest]("abc").right.value.value shouldBe "abc"
  }

  property("creating an invalid ValidatedString should fail with ValidationError") {
    ValidatedStringConstructor[ValidatedStringTest]("abXba").left.value shouldBe ValidationError("The value can't contain 'X'.")
  }

}
