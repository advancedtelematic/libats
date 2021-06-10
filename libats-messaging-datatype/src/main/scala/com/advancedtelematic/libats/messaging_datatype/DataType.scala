package com.advancedtelematic.libats.messaging_datatype

import com.advancedtelematic.libats.data.DataType.{ResultCode, ResultDescription}
import com.advancedtelematic.libats.data.UUIDKey.{UUIDKey, UUIDKeyObj}
import eu.timepit.refined.api.{Refined, Validate}
import io.circe.Json

import java.time.Instant
import java.util.UUID

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

  /**
    * Status of a device in the campaign:
    * - `requested` when a device is initially added to the campaign (corresponds
    *   to `processed` state in the UI until a device goes to the Director)
    * - `rejected` when a device is rejected by Director and is not a part of the
    *    campaign anymore (corresponds to `not impacted` state in the UI)
    * - `scheduled` when a device is approved by Director and is scheduled for
    *    an update (partly corresponds to `queued` state in the UI)
    * - `accepted` when an update was accepted on a device and is about to be
    *    installed (partly corresponds to `queued` state in the UI)
    * - `successful` when a device update was applied successfully
    * - `cancelled` when a device update was cancelled
    * - `failed` when a device update was failed
    */
  object DeviceStatus extends Enumeration {
    type DeviceStatus = Value

    val requested, rejected, scheduled, accepted, successful, cancelled, failed = Value
  }

  final case class CampaignId(uuid: UUID) extends UUIDKey
  object CampaignId extends UUIDKeyObj[CampaignId]

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
