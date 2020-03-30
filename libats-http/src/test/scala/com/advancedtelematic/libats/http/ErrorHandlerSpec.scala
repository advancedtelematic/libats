package com.advancedtelematic.libats.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.advancedtelematic.libats.data.ErrorRepresentation
import ErrorRepresentation._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.CursorOp.DownField
import io.circe.DecodingFailure
import org.scalatest.{FunSuite, Matchers}

class ErrorHandlerSpec extends FunSuite with Matchers with ScalatestRouteTest {
  import akka.http.scaladsl.server.Directives._

  val routes: Route = ErrorHandler.handleErrors {
    path("error") {
      throw new RuntimeException("Some error")
    } ~
    path("ok") {
      complete("ok")
    }
  }

  test("DecodingFailure error handler keeps decoding history") {
    val (_, errorRepresentation) = Errors.onDecodingError(DecodingFailure("msg", List(DownField("field"))))
    assert(errorRepresentation.description == "msg: DownField(field)")
  }

  test("uses application/json content-type when returning error") {

    Get("/ok") ~> routes ~> check {
      status shouldBe StatusCodes.OK
    }

    Get("/error") ~> routes ~> check {
      status shouldBe StatusCodes.InternalServerError
      responseAs[ErrorRepresentation].description shouldBe "Some error"
    }
  }
}
