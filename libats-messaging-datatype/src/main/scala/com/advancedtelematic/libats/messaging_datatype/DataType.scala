package com.advancedtelematic.libats.messaging_datatype

import java.time.Instant
import java.util.UUID

import com.advancedtelematic.libats.data.DataType.HashMethod.HashMethod
import com.advancedtelematic.libats.data.DataType.ValidChecksum
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

  @deprecated("use data type from libtuf-server", "0.0.1-109")
  case class ValidTargetFilename()
  @deprecated("use data type from libtuf-server", "v0.1.1-21")
  type TargetFilename = Refined[String, ValidTargetFilename]

  @deprecated("use data type from libtuf-server", "v0.1.1-21")
  implicit val validTargetFilename: Validate.Plain[String, ValidTargetFilename] =
    Validate.fromPredicate(
      f => f.nonEmpty && f.length < 254,
      _ => "TargetFilename cannot be empty or bigger than 254 chars",
      ValidTargetFilename()
    )

  @deprecated("use data type from libtuf-server", "v0.1.1-21")
  final case class OperationResult(target: TargetFilename, hashes: Map[HashMethod, Refined[String, ValidChecksum]],
                                   length: Long, resultCode: Int, resultText: String) {
    def isSuccess:Boolean = resultCode == 0 || resultCode == 1
  }

  final case class InstallationResult(success: Boolean, code: String, description: String)

  final case class EcuInstallationReport(result: InstallationResult, target: Seq[String], rawReport: Option[Array[Byte]] = None)

  final case class EventType(id: String, version: Int)

  final case class Event(deviceUuid: DeviceId,
                         eventId: String,
                         eventType: EventType,
                         deviceTime: Instant,
                         receivedAt: Instant,
                         payload: Json)
}
