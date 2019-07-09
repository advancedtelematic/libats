package com.advancedtelematic.libats.logging

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Assertion, FunSuite, Matchers}

class RawMessageFormatterSpec extends FunSuite with Matchers
  with GeneratorDrivenPropertyChecks with MessageFormatterGen {

  private def helper(remoteAddr: String,
                     requestMethod: String,
                     requestUri: String,
                     responseContentLength: Int,
                     responseStatusCode: Int,
                     replyTimeMs: Long)(checker: String => Assertion): Assertion = {
    val message = new RawMessageFormatter("test-service")
      .getMessage(
        remoteAddr,
        requestMethod,
        requestUri,
        responseContentLength,
        responseStatusCode,
        replyTimeMs)
    checker(message)
  }

  test("Raw message has remote_addr field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) {
          _ should include(s"remote_addr=${values._1}")
        }
    }
  }

  test("Raw message has method field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) {
          _ should include(s"method=${values._2}")
        }
    }
  }

  test("Raw message has path field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) { message: String =>
          message should include(s"path=${values._3}")
        }
    }
  }

  test("Raw message has content_ln field serialized") {
    forAll(gen) {
      values =>
        whenever(values._4 > 0) {
          (helper _).tupled(values) {
            _ should include(s"content_ln=${values._4}")
          }
        }
    }
  }

  test("Raw message has no content_ln field") {
    forAll(gen) {
      values =>
        (helper _).tupled(values.copy(_4 = 0)) {
          _ shouldNot include(s"content_ln")
        }
    }
  }

  test("Raw message has status field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) {
          _ should include(s"status=${values._5}")
        }
    }
  }

  test("Raw message has stime field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) {
          _ should include(s"stime=${values._6}")
        }
    }
  }

}

