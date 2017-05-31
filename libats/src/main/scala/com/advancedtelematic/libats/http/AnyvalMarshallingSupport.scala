package com.advancedtelematic.libats.http

import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller

object AnyvalMarshallingSupport {
  import shapeless._

  private def anyvalFromEntityUnmarshaller[Wrapper <: AnyVal, Wrapped](implicit gen: Generic.Aux[Wrapper, Wrapped :: HNil],
                                                                       um: FromStringUnmarshaller[Wrapped]): FromStringUnmarshaller[Wrapper] =
    um.map { v ⇒ gen.from(v :: HNil) }

  implicit def anyvalStringUnmarshaller[Wrapper <: AnyVal]
  (implicit gen: Generic.Aux[Wrapper, String :: HNil]): FromStringUnmarshaller[Wrapper] = anyvalFromEntityUnmarshaller[Wrapper, String]
}
