package com.advancedtelematic.libats.http

import akka.http.scaladsl.server.{PathMatcher1, PathMatchers}
import com.advancedtelematic.libats.data.UUIDKey.{UUIDKey, UUIDKeyObj}
import shapeless._

object UUIDKeyPath {
  implicit class UUIDKeyPathOp[Self <: UUIDKey](value: UUIDKeyObj[Self]) {
    def Path(implicit gen: value.SelfGen): PathMatcher1[Self] = {
      PathMatchers.JavaUUID.map { uuid =>
        gen.from(uuid :: HNil)
      }
    }
  }
}
