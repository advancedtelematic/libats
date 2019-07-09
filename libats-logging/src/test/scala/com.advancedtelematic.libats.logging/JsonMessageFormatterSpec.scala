package com.advancedtelematic.libats.logging

import io.circe._
import io.circe.parser._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Assertion, FunSuite, Matchers}

class JsonMessageFormatterSpec extends FunSuite with Matchers
  with GeneratorDrivenPropertyChecks with MessageFormatterGen {

  private def helper(remoteAddr: String,
                     requestMethod: String,
                     requestUri: String,
                     responseContentLength: Int,
                     responseStatusCode: Int,
                     replyTimeMs: Long)(checker: Json => Assertion): Assertion = {
    val message = new JsonMessageFormatter("test-service")
      .getMessage(
        remoteAddr,
        requestMethod,
        requestUri,
        responseContentLength,
        responseStatusCode,
        replyTimeMs)
    checker(parse(message).getOrElse(Json.Null))
  }

  test("Json message has remote_addr field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) {
          _.hcursor.get[String]("remote_addr") shouldBe Right(values._1)
        }
    }
  }

  test("Json message has method field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) {
          _.hcursor.get[String]("method") shouldBe Right(values._2)
        }
    }
  }

  test("Json message has path field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) {
          _.hcursor.get[String]("path") shouldBe Right(values._3)
        }
    }
  }

  test("Json message has content_ln field serialized") {
    forAll(gen) {
      values =>
        whenever(values._4 > 0) {
          (helper _).tupled(values) {
            _.hcursor.get[String]("content_ln") shouldBe Right(values._4.toString)
          }
        }
    }
  }

  test("Json message has no content_ln field") {
    forAll(gen) {
      values =>
        (helper _).tupled(values.copy(_4 = 0)) {
          _.hcursor.get[String]("content_ln").left.get.message shouldBe
            "Attempt to decode value on failed cursor"
        }
    }
  }

  test("Json message has status field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) {
          _.hcursor.get[String]("status") shouldBe Right(values._5.toString)
        }
    }
  }

  test("Json message has stime field serialized") {
    forAll(gen) {
      values =>
        (helper _).tupled(values) {
          _.hcursor.get[String]("stime") shouldBe Right(values._6.toString)
        }
    }
  }

}

