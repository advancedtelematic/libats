package com.advancedtelematic.libats.http

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import com.advancedtelematic.libats.data.{Limit, Offset}
import com.advancedtelematic.libats.http.FromLongUnmarshallers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class FromLongUnmarshallersSpec extends FunSuite with Matchers with ScalaFutures {
  implicit val system: ActorSystem = ActorSystem("FromLongUnmarshallersSpec")
  val MaxLimit = 10
  implicit val limitUnmarshaller: Unmarshaller[String, Limit] = getLimitUnmarshaller(MaxLimit)

  test("returns unmarshalled offset when offset is not negative") {
    val offset = Unmarshal("1").to[Offset]
    whenReady(offset) { result =>
      result shouldEqual Offset(1)
    }
  }

  test("returns error when offset is negative") {
    val offset = Unmarshal("-1").to[Offset]
    whenReady(offset.failed) { result =>
      result shouldBe a [IllegalArgumentException]
      result should have message "Offset cannot be negative"
    }
  }

  test("returns unmarshalled limit when limit is not negative and less than maximum limit") {
    val limit = Unmarshal("1").to[Limit]
    whenReady(limit) { result =>
      result shouldEqual Limit(1)
    }
  }

  test("returns error when limit is negative") {
    val limit = Unmarshal("-1").to[Limit]
    whenReady(limit.failed) { result =>
      result shouldBe a [IllegalArgumentException]
      result should have message s"Limit cannot be negative or greater than $MaxLimit"
    }
  }

  test("returns error when limit is greater than maximum limit") {
    val limit = Unmarshal("11").to[Limit]
    whenReady(limit.failed) { result =>
      result shouldBe a [IllegalArgumentException]
      result should have message s"Limit cannot be negative or greater than $MaxLimit"
    }
  }
}
