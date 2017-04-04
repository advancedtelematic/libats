package com.advancedtelematic.libats.messaging_datatype

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.Uri
import com.advancedtelematic.libats.data.Namespace
import com.advancedtelematic.libats.data.UUIDKey.{UUIDKey, UUIDKeyObj}
import eu.timepit.refined.api.{Refined, Validate}

object DataType {
  case class PackageId(name: String, version: String) {
    def mkString = s"$name-$version"
  }

  case class ValidCommit()
  type Commit = Refined[String, ValidCommit]

  implicit val validCommit: Validate.Plain[String, ValidCommit] =
    Validate.fromPredicate(
      hash => hash.forall(h => ('0' to '9').contains(h) || ('a' to 'f').contains(h)),
      hash => s"$hash is not a sha-256 commit hash",
      ValidCommit()
    )

  case class DeltaRequestId(uuid: UUID) extends UUIDKey
  object DeltaRequestId extends UUIDKeyObj[DeltaRequestId]

  case class OstreeSummary(bytes: Array[Byte])


}
