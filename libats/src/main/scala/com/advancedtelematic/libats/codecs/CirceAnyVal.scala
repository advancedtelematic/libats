package com.advancedtelematic.libats.codecs

import io.circe.{Decoder, Encoder}

trait CirceAnyVal {

  import shapeless._

  private def anyValEncoder[Wrapper <: AnyVal, Wrapped](implicit gen: Generic.Aux[Wrapper, Wrapped :: HNil],
                                                         wrappedEncoder: Encoder[Wrapped]): Encoder[Wrapper] =
    wrappedEncoder.contramap[Wrapper](a => gen.to(a).head)

  private def anyValDecoder[Wrapper <: AnyVal, Wrapped](implicit gen: Generic.Aux[Wrapper, Wrapped :: HNil],
                                                         wrappedDecoder: Decoder[Wrapped]): Decoder[Wrapper] =
    wrappedDecoder.map { x =>
      gen.from(x :: HNil)
    }

  implicit def anyValStringEncoder[Wrapper <: AnyVal]
  (implicit gen: Generic.Aux[Wrapper, String :: HNil]): Encoder[Wrapper] = anyValEncoder[Wrapper, String]

  implicit def anyValStringDecoder[Wrapper <: AnyVal]
  (implicit gen: Generic.Aux[Wrapper, String :: HNil]): Decoder[Wrapper] = anyValDecoder[Wrapper, String]
}

object CirceAnyVal extends CirceAnyVal
