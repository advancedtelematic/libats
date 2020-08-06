package com.advancedtelematic.libats.messaging_datatype

import java.time.Instant
import java.util.UUID

import com.advancedtelematic.libats.data.DataType.HashMethod.HashMethod
import com.advancedtelematic.libats.data.DataType.{ResultCode, ResultDescription, ValidChecksum}
import com.advancedtelematic.libats.data.UUIDKey.{UUIDKey, UUIDKeyObj}
import eu.timepit.refined.api.{Refined, Validate}
import io.circe.Json

object DataType {
  case class PackageId(name: String, version: String) {
    def mkString = s"$name-$version"
  }

  case class DeltaRequestId(uuid: UUID) extends UUIDKey
  object DeltaRequestId extends UUIDKeyObj[DeltaRequestId]

  case class BsDiffRequestId(uuid: UUID) extends UUIDKey
  object BsDiffRequestId extends UUIDKeyObj[BsDiffRequestId]

  object UpdateType extends Enumeration {
    type UpdateType = Value

    val Image, Package = Value
  }

  import com.advancedtelematic.libats.data.ValidationUtils._
  case class ValidCommit()

  type Commit = Refined[String, ValidCommit]

  implicit val validCommit: Validate.Plain[String, ValidCommit] =
    Validate.fromPredicate(
      hash => validHex(64, hash),
      hash => s"$hash is not a sha-256 commit hash",
      ValidCommit()
    )

  case class DeviceId(uuid: UUID) extends UUIDKey
  object DeviceId extends UUIDKeyObj[DeviceId]

  case class UpdateId(uuid: UUID) extends UUIDKey
  object UpdateId extends UUIDKeyObj[UpdateId]

  final case class SourceUpdateId(value: String) extends AnyVal

  final case class InstallationResult(success: Boolean, code: ResultCode, description: ResultDescription)

  final case class EcuInstallationReport(result: InstallationResult, target: Seq[String], rawReport: Option[Array[Byte]] = None)

  final case class EventType(id: String, version: Int)

  final case class Event(deviceUuid: DeviceId,
                         eventId: String,
                         eventType: EventType,
                         deviceTime: Instant,
                         receivedAt: Instant,
                         payload: Json)
}
