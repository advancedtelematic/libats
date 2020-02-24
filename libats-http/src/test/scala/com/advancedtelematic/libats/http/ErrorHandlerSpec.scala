package com.advancedtelematic.libats.http

import io.circe.CursorOp.DownField
import io.circe.DecodingFailure
import org.scalatest.FunSuite

class ErrorHandlerSpec extends FunSuite {

  test("DecodingFailure error handler keeps decoding history") {
    val (_, errorRepresentation) = Errors.onDecodingError(DecodingFailure("msg", List(DownField("field"))))
    assert(errorRepresentation.description == "msg: DownField(field)")
  }

}
