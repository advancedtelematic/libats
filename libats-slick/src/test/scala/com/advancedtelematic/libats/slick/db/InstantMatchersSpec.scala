package com.advancedtelematic.libats.slick.db

import java.time.Instant

import com.advancedtelematic.libats.test.InstantMatchers
import org.scalatest.{FunSuite, Matchers}

class InstantMatchersSpec extends FunSuite with Matchers with InstantMatchers {
   test("be before") {
     Instant.now() shouldBe before(Instant.now().plusSeconds(30))
   }

  test("be after") {
    Instant.now() shouldBe after(Instant.now().minusSeconds(30))
  }
}
