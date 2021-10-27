package com.advancedtelematic.libats.messaging_datatype

import cats.syntax.show._
import com.advancedtelematic.libats.codecs.CirceValidatedGeneric
import com.advancedtelematic.libats.data.DataType.{Checksum, CorrelationId, Namespace, ResultCode, ResultDescription}
import com.advancedtelematic.libats.data.EcuIdentifier
import com.advancedtelematic.libats.messaging_datatype.DataType.DeviceStatus.DeviceStatus
import com.advancedtelematic.libats.messaging_datatype.DataType.UpdateType.UpdateType
import com.advancedtelematic.libats.messaging_datatype.DataType._
import com.advancedtelematic.libats.messaging_datatype.Messages.{BsDiffGenerationFailed, BsDiffRequest, CampaignLaunched, CampaignUpdateEvent, DeltaGenerationFailed, DeltaRequest, DeviceEventMessage, DeviceSystemInfoChanged, DeviceUpdateAssigned, DeviceUpdateCanceled, DeviceUpdateCompleted, DeviceUpdateEvent, EcuAndHardwareId, EcuReplaced, EcuReplacement, EcuReplacementFailed, GeneratedBsDiff, GeneratedDelta, OSTreeTargetDelete, SystemInfo, UserCreated}
import io.circe.{Json, _}
import io.circe.generic.semiauto._
import io.circe.syntax._

import java.net.URI
import java.time.Instant
import java.util.UUID

object MessageCodecs {
  import com.advancedtelematic.libats.codecs.CirceCodecs._

  implicit val eventTypeCodec: Codec[EventType] = deriveCodec
  implicit val eventCodec: Codec[Event] = deriveCodec

  implicit val deviceEventEncoder: Encoder[DeviceEventMessage] = Encoder.instance { x =>
    eventCodec(x.event).mapObject(_.add("namespace", x.namespace.get.asJson))
  }

  implicit val deviceEventDecoder: Decoder[DeviceEventMessage] = Decoder.instance { c =>
    for {
      event <- c.as[Event]
      ns    <- c.get[String]("namespace").map(Namespace.apply)
    } yield DeviceEventMessage(ns, event)
  }

  implicit val ecuIdentifierKeyCodec: KeyEncoder[EcuIdentifier] = CirceValidatedGeneric.validatedGenericKeyEncoder[EcuIdentifier, String]
  implicit val ecuIdentifierKeyDecoder: KeyDecoder[EcuIdentifier] = CirceValidatedGeneric.validatedGenericKeyDecoder[EcuIdentifier, String]

  implicit val deviceUpdateEventCodec: Codec[DeviceUpdateEvent] = deriveCodec
  implicit val deviceUpdateAvailableCodec: Codec[DeviceUpdateAssigned] = deriveCodec
  implicit val deviceUpdateCanceledCodec: Codec[DeviceUpdateCanceled] = deriveCodec
  implicit val deviceUpdateCompletedCodec: Codec[DeviceUpdateCompleted] = deriveCodec
  implicit val userCreatedCodec: Codec[UserCreated] = deriveCodec
  implicit val campaignLaunchedCodec: Codec[CampaignLaunched] = deriveCodec
  implicit val packageIdCodec: Codec[PackageId] = deriveCodec
  implicit val deltaRequestCodec: Codec[DeltaRequest] = deriveCodec
  implicit val generatedDeltaCodec: Codec[GeneratedDelta] = deriveCodec
  implicit val bsDiffRequestIdCodec: Codec[BsDiffRequest] = deriveCodec
  implicit val generatedBsDiffCodec: Codec[GeneratedBsDiff] = deriveCodec
  implicit val deltaGenerationFailedCodec: Codec[DeltaGenerationFailed] = deriveCodec
  implicit val bsDiffGenerationFailedCodec: Codec[BsDiffGenerationFailed] = deriveCodec
  implicit val installationResultCodec: Codec[InstallationResult] = deriveCodec
  implicit val ecuInstallationReportCodec: Codec[EcuInstallationReport] = deriveCodec
  implicit val updateTypeCodec: Codec[UpdateType] = Codec.codecForEnumeration(UpdateType)
  implicit val sourceUpdateIdCodec: Codec[SourceUpdateId] = deriveCodec
  implicit val systemInfoCodec: Codec[SystemInfo] = deriveCodec
  implicit val deviceSystemInfoChangedCodec: Codec[DeviceSystemInfoChanged] = deriveCodec
  implicit val ecuAndHardwareIdCodec: Codec[EcuAndHardwareId] = deriveCodec
  implicit val deviceStatusCodec: Codec[DeviceStatus] = Codec.codecForEnumeration(DeviceStatus)
  implicit val campaignUpdateEventCodec: Codec[CampaignUpdateEvent] = deriveCodec
  implicit val osTreeTargetDeleteCodec: Codec[OSTreeTargetDelete] = deriveCodec

  implicit val ecuReplacementCodec: Codec[EcuReplacement] = Codec.from(
    Decoder.instance { c =>
      c.get[Boolean]("success").flatMap {
        case true => deriveDecoder[EcuReplaced].tryDecode(c)
        case false => deriveDecoder[EcuReplacementFailed].tryDecode(c)
      }
    },
    Encoder.instance {
      case e: EcuReplaced => deriveEncoder[EcuReplaced].apply(e).mapObject(("success", Json.fromBoolean(true)) +: _)
      case e: EcuReplacementFailed => deriveEncoder[EcuReplacementFailed].apply(e).mapObject(("success", Json.fromBoolean(false)) +: _)
    }
  )
}

object Messages {
  import MessageCodecs._
  import com.advancedtelematic.libats.codecs.CirceCodecs._

  final case class UserCreated(id: String)

  final case class DeviceSeen(namespace: Namespace, uuid: DeviceId, lastSeen: Instant = Instant.now)

  final case class CampaignLaunched(namespace: String, updateId: UUID,
                                    devices: Set[UUID], pkgUri: URI,
                                    pkg: PackageId, pkgSize: Long, pkgChecksum: String)

  case class DeltaRequest(id: DeltaRequestId, namespace: Namespace, from: Commit, to: Commit, timestamp: Instant = Instant.now)

  case class BsDiffRequest(id: BsDiffRequestId, namespace: Namespace, from: URI, to: URI, timestamp: Instant = Instant.now)

  case class GeneratedDelta(id: DeltaRequestId, namespace: Namespace, from: Commit, to: Commit, uri: URI, size: Long, checksum: Checksum)

  case class GeneratedBsDiff(id: BsDiffRequestId, namespace: Namespace, from: URI, to: URI, resultUri: URI, size: Long, checksum: Checksum)

  case class DeltaGenerationFailed(id: DeltaRequestId, namespace: Namespace, error: Option[Json] = None)

  case class BsDiffGenerationFailed(id: BsDiffRequestId, namespace: Namespace, error: Option[Json] = None)

  final case class DeviceEventMessage(namespace: Namespace, event: Event)

  case class BandwidthUsage(id: UUID, namespace: Namespace, timestamp: Instant, byteCount: Long,
                            updateType: UpdateType, updateId: String)

  case class ImageStorageUsage(namespace: Namespace, timestamp: Instant, byteCount: Long)

  sealed trait DeviceUpdateEvent {
    def namespace: Namespace
    def eventTime: Instant
    def correlationId: CorrelationId
    def deviceUuid: DeviceId
  }

  final case class DeviceUpdateAssignmentRequested(
      namespace: Namespace,
      eventTime: Instant,
      correlationId: CorrelationId,
      deviceUuid: DeviceId,
      sourceUpdateId: SourceUpdateId
  ) extends DeviceUpdateEvent

  final case class DeviceUpdateAssigned(
      namespace: Namespace,
      eventTime: Instant,
      correlationId: CorrelationId,
      deviceUuid: DeviceId
  ) extends DeviceUpdateEvent

  final case class DeviceUpdateAssignmentRejected(
      namespace: Namespace,
      eventTime: Instant,
      correlationId: CorrelationId,
      deviceUuid: DeviceId
  ) extends DeviceUpdateEvent

  final case class DeviceUpdateCancelRequested(
      namespace: Namespace,
      eventTime: Instant,
      correlationId: CorrelationId,
      deviceUuid: DeviceId
  ) extends DeviceUpdateEvent

  final case class DeviceUpdateCanceled(
      namespace: Namespace,
      eventTime: Instant,
      correlationId: CorrelationId,
      deviceUuid: DeviceId
  ) extends DeviceUpdateEvent

  final case class DeviceUpdateCompleted(
      namespace: Namespace,
      eventTime: Instant,
      correlationId: CorrelationId,
      deviceUuid: DeviceId,
      result: InstallationResult,
      ecuReports: Map[EcuIdentifier, EcuInstallationReport],
      rawReport: Option[String] = None
  ) extends DeviceUpdateEvent

  final case class SystemInfo(product: Option[String])

  final case class DeviceSystemInfoChanged(namespace: Namespace, uuid: DeviceId, newSystemInfo: Option[SystemInfo])

  final case class CommitManifestUpdated(namespace: Namespace, commit: Commit, releaseBranch: String,
                                         metaUpdaterVersion: String, receivedAt: Instant = Instant.now())

  final case class AktualizrConfigChanged(namespace: Namespace, uuid: DeviceId, pollingSec: Int,
                                          secondaryPreinstallWaitSec: Option[Int], forceInstallCompletion: Boolean,
                                          installerType: String, receivedAt: Instant)

  final case class DeleteDeviceRequest(namespace: Namespace, uuid: DeviceId, timestamp: Instant = Instant.now())

  sealed trait EcuReplacement {
    val deviceUuid: DeviceId
    val eventTime: Instant
  }
  case class EcuAndHardwareId(ecuId: EcuIdentifier, hardwareId: String)
  final case class EcuReplaced(deviceUuid: DeviceId, former: EcuAndHardwareId, current: EcuAndHardwareId, eventTime: Instant = Instant.now) extends EcuReplacement
  final case class EcuReplacementFailed(deviceUuid: DeviceId, eventTime: Instant = Instant.now) extends EcuReplacement

  case class CampaignUpdateEvent(
      namespace: Namespace,
      deviceId: DeviceId,
      campaignId: CampaignId,
      deviceStatus: DeviceStatus,
      resultCode: Option[ResultCode],
      resultDescription: Option[ResultDescription],
      updatedAt: Instant)

  final case class OSTreeTargetDelete(namespace: Namespace)

  implicit val osTreeTargetDeleteMessageLike = MessageLike[OSTreeTargetDelete](_.namespace.get)

  implicit val deviceSystemInfoChangedMessageLike = MessageLike.derive[DeviceSystemInfoChanged](_.uuid.toString)

  implicit val commitManifestUpdatedMessageLike = MessageLike.derive[CommitManifestUpdated](_.commit.value)

  implicit val aktualizrConfigChangedMessageLike = MessageLike.derive[AktualizrConfigChanged](_.uuid.toString)

  implicit val userCreatedMessageLike = MessageLike[UserCreated](_.id)

  implicit val deviceSeenMessageLike = MessageLike.derive[DeviceSeen](_.uuid.toString)

  implicit val campaignLaunchedMessageLike = MessageLike[CampaignLaunched](_.updateId.toString)

  implicit val staticDeltaRequestMessageLike = MessageLike[DeltaRequest](_.id.uuid.toString)

  implicit val bsDiffRequestMessageLike = MessageLike[BsDiffRequest](_.id.uuid.toString)

  implicit val staticDeltaResponseMessageLike = MessageLike[GeneratedDelta](_.id.uuid.toString)

  implicit val generatedBsDiffMessageLike = MessageLike[GeneratedBsDiff](_.id.uuid.toString)

  implicit val bsDiffGenerationFailedMessageLike = MessageLike[BsDiffGenerationFailed](_.id.toString)

  implicit val deltaGenerationFailedMessageLike = MessageLike[DeltaGenerationFailed](_.id.uuid.toString)

  implicit val bandwidthUsageMessageLike = MessageLike.derive[BandwidthUsage](_.id.toString)

  implicit val imageStorageMessageLike = MessageLike.derive[ImageStorageUsage](_.namespace.get)

  implicit val deviceEventMessageType = MessageLike[DeviceEventMessage](_.namespace.get)

  implicit val deviceUpdateEventType = MessageLike[DeviceUpdateEvent](_.namespace.get)

  implicit val deleteDeviceRequestMessageLike = MessageLike.derive[DeleteDeviceRequest](_.uuid.show)

  implicit val ecuReplacementMsgLike = MessageLike[EcuReplacement](_.deviceUuid.show)

  implicit val campaignUpdateEventMsgLike = MessageLike[CampaignUpdateEvent](_.campaignId.toString)
}
