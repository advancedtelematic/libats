package com.advancedtelematic.libats.data

import org.scalatest.{EitherValues, Matchers, PropSpec}

class ValidatedStringSpec extends PropSpec with Matchers with EitherValues {

  property("creating an invalid StringSmartConstructor should fail with ValidationError") {
    SmartStringConstructor[ValidatedString]("abXba").left.value shouldBe ValidationError("The value can't contain 'X'.")
  }

}
