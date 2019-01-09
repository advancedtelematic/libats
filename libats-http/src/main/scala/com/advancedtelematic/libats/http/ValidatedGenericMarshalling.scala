package com.advancedtelematic.libats.http

import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.data.ValidatedGeneric

object ValidatedGenericMarshalling {

  implicit def validatedStringUnmarshaller[T](implicit gen: ValidatedGeneric[T, String]): FromStringUnmarshaller[T] =
    Unmarshaller[String, T](_ => s => gen.from(s) match {
      case Left(e) => FastFuture.failed(e)
      case Right(t) => FastFuture.successful(t)
    })

}
