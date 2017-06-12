package com.advancedtelematic.libats.data

import java.util.UUID

import akka.http.scaladsl.server.{PathMatcher1, PathMatchers}
import cats.Show
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import shapeless._

object UUIDKey {
  abstract class UUIDKeyObj[Self <: UUIDKey] {

    type SelfGen = Generic.Aux[Self, UUID :: HNil]

    def generate()(implicit gen: SelfGen): Self =
      fromJava(UUID.randomUUID())

    private def fromJava(value: UUID)(implicit gen: SelfGen): Self = {
      gen.from(value :: HNil)
    }

    implicit val encoder: Encoder[Self] = Encoder[String].contramap(_.uuid.toString)

    implicit def decoder(implicit gen: SelfGen): Decoder[Self] =
      Decoder[String].map(s => fromJava(UUID.fromString(s)))

    implicit def keyDecoder(implicit gen: SelfGen): KeyDecoder[Self] =
      KeyDecoder[String].map(s => gen.from(UUID.fromString(s) :: HNil))

    implicit def keyEncoder: KeyEncoder[Self] = KeyEncoder[String].contramap(_.uuid.toString)

    implicit val abstractKeyShow = Show.show[Self](_.uuid.toString)

    def Path(implicit gen: Generic.Aux[Self, UUID :: HNil]): PathMatcher1[Self] =
      PathMatchers.JavaUUID.map(fromJava)
  }

  abstract class UUIDKey {
    val uuid: UUID
  }
}
