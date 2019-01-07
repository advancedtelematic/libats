package com.advancedtelematic.libats.http

import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.data.SmartStringConstructor

object SmartConstructorMarshallingSupport {

  implicit def smartConstructorStringUnmarshaller[T](implicit cons: SmartStringConstructor[T]): FromStringUnmarshaller[T] =
    Unmarshaller[String, T](_ => s => cons.apply(s) match {
      case Left(e) => FastFuture.failed(e)
      case Right(v) => FastFuture.successful(v)
    })

}
