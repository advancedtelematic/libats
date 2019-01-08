package com.advancedtelematic.libats.http

import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.data.ValidatedStringConstructor

object ValidatedStringMarshallingSupport {

  implicit def validatedStringUnmarshaller[T](implicit cons: ValidatedStringConstructor[T]): FromStringUnmarshaller[T] =
    Unmarshaller[String, T](_ => s => cons.apply(s) match {
      case Left(e) => FastFuture.failed(e)
      case Right(v) => FastFuture.successful(v)
    })

}
