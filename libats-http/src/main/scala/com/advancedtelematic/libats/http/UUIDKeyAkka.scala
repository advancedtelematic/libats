package com.advancedtelematic.libats.http

import java.util.UUID

import akka.http.scaladsl.server.{PathMatcher1, PathMatchers}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.advancedtelematic.libats.data.UUIDKey.{UUIDKey, UUIDKeyObj}
import shapeless._

object UUIDKeyAkka {
  implicit class UUIDKeyUnmarshallerOp[Self <: UUIDKey](value: UUIDKeyObj[Self]) {
    def unmarshaller[T <: UUIDKey](implicit gen: value.SelfGen): Unmarshaller[String, Self] =
      Unmarshaller.strict { str => gen.from(UUID.fromString(str) :: HNil) }
  }

  implicit class UUIDKeyPathOp[Self <: UUIDKey](value: UUIDKeyObj[Self]) {
    def Path(implicit gen: value.SelfGen): PathMatcher1[Self] = {
      PathMatchers.JavaUUID.map { uuid =>
        gen.from(uuid :: HNil)
      }
    }
  }
}
