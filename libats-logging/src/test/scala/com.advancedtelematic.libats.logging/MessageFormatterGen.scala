package com.advancedtelematic.libats.logging

import org.scalacheck.Gen

trait MessageFormatterGen {

  def gen: Gen[(String, String, String, Int, Int, Long)] = for {
    remoteAddr <- Gen.alphaNumStr
    requestMethod <- Gen.oneOf("GET", "PUT", "POST", "DELETE", "PATCH")
    requestUri <- Gen.listOf(Gen.alphaNumStr.suchThat(!_.isEmpty)).map(s => s"${s.mkString("/")}")
    responseContentLength <- Gen.choose[Int](0, 1000000)
    responseStatusCode <- Gen.oneOf(200, 302, 400, 404, 500)
    replyTimeMs <- Gen.choose[Long](0, 100)
  } yield (remoteAddr,
    requestMethod,
    requestUri,
    responseContentLength,
    responseStatusCode,
    replyTimeMs)

}
