package com.advancedtelematic.libats.http

import org.scalatest.FunSuite
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes._

class DefaultRejectionHandlerSpec extends FunSuite with ScalatestRouteTest {
  test("Doesn't leak user-defined data") {
    val route =
      handleRejections(DefaultRejectionHandler.rejectionHandler) {
        parameter('count.as[Int]) { count =>
          complete(s"you have $count of it.")
        }
    }

    Get("/?count=userDefined") ~> route ~> check {
      assert(status == BadRequest)

      val r = responseAs[String]
      assert(r.startsWith("{\"code\":\"invalid_entity\",\"description\":\"The query parameter 'count' was malformed\""))
      assert(!r.contains("userDefined"))
    }
  }
}
