package com.advancedtelematic.libats.data

import java.util.UUID

import akka.http.scaladsl.server.{PathMatcher1, PathMatchers}
import cats.Show
import io.circe.{Decoder, Encoder}
import shapeless._

import scala.reflect.ClassTag

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

    implicit val abstractKeyShow = Show.show[Self](_.uuid.toString)

    def Path(implicit gen: Generic.Aux[Self, UUID :: HNil]): PathMatcher1[Self] =
      PathMatchers.JavaUUID.map(fromJava)
  }

  abstract class UUIDKey {
    val uuid: UUID
  }
}
