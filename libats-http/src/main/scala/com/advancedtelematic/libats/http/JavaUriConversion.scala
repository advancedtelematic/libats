package com.advancedtelematic.libats.http

import java.net.URI

import akka.http.scaladsl.model.Uri

import scala.language.implicitConversions

object JavaUriConversion {
  implicit def javaUriToAkkaUriConversion(value: URI): Uri = Uri(value.toString)
}
