package com.advancedtelematic.libats.http

import akka.http.scaladsl.model.Uri
import io.circe.{Decoder, Encoder}

object HttpCodecs {
  implicit val uriEncoder: Encoder[Uri] = Encoder[String].contramap(_.toString)
  implicit val uriDecoder: Decoder[Uri] = Decoder[String].map(Uri.apply)
}


