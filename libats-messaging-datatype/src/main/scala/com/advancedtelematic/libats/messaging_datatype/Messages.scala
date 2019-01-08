
package com.advancedtelematic.libats.messaging_datatype

import java.net.URI
import java.time.Instant
import java.util.UUID

import com.advancedtelematic.libats.codecs.CirceSmartConstructor.{keyDecoderEcuIdentifier, keyEncoderEcuIdentifier}
import com.advancedtelematic.libats.data.DataType.{Checksum, CorrelationId, Namespace}
import com.advancedtelematic.libats.data.EcuIdentifier
import com.advancedtelematic.libats.messaging_datatype.DataType.DeviceUpdateStatus.DeviceUpdateStatus
import com.advancedtelematic.libats.messaging_datatype.DataType.UpdateType.UpdateType
import com.advancedtelematic.libats.messaging_datatype.DataType._
import com.advancedtelematic.libats.messaging_datatype.Messages._
import com.advancedtelematic.libats.messaging_datatype.Messages.DeviceInstallationReport
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

object MessageCodecs {
  import com.advancedtelematic.libats.codecs.CirceCodecs._

  implicit val eventTypeEncoder: Encoder[EventType] = deriveEncoder
  implicit val eventTypeDecoder: Decoder[EventType] = deriveDecoder

  implicit val eventEncoder: Encoder[Event] = deriveEncoder[Event]
  implicit val eventDecoder: Decoder[Event] = deriveDecoder[Event]

  implicit val deviceEventEncoder: Encoder[DeviceEventMessage] = Encoder.instance { x =>
    eventEncoder(x.event).mapObject(_.add("namespace", x.namespace.get.asJson))
  }

  implicit val deviceEventDecoder: Decoder[DeviceEventMessage] = Decoder.instance { c =>
    for {
      event <- c.as[Event]
      ns    <- c.get[String]("namespace").map(Namespace.apply)
    } yield DeviceEventMessage(ns, event)
  }

  implicit val deviceUpdateStatusEncoder: Encoder[DeviceUpdateStatus] = Encoder.enumEncoder(DeviceUpdateStatus)
  implicit val deviceUpdateStatusDecoder: Decoder[DeviceUpdateStatus] = Decoder.enumDecoder(DeviceUpdateStatus)

  implicit val deviceUpdateEventEncoder: Encoder[DeviceUpdateEvent] = deriveEncoder[DeviceUpdateEvent]
  implicit val deviceUpdateEventDecoder: Decoder[DeviceUpdateEvent] = deriveDecoder[DeviceUpdateEvent]

  implicit val userCreatedEncoder: Encoder[UserCreated] = deriveEncoder
  implicit val userCreatedDecoder: Decoder[UserCreated] = deriveDecoder

  implicit val campaignLaunchedEncoder: Encoder[CampaignLaunched] = deriveEncoder
  implicit val campaignLaunchedDecoder: Decoder[CampaignLaunched] = deriveDecoder

  implicit val packageIdEncoder: Encoder[PackageId] = deriveEncoder
  implicit val packageIdDecoder: Decoder[PackageId] = deriveDecoder

  implicit val deltaRequestEncoder: Encoder[DeltaRequest] = deriveEncoder
  implicit val deltaRequestDecoder: Decoder[DeltaRequest] = deriveDecoder

  implicit val generatedDeltaEncoder: Encoder[GeneratedDelta] = deriveEncoder
  implicit val generatedDeltaDecoder: Decoder[GeneratedDelta] = deriveDecoder

  implicit val bsDiffRequestIdEncoder: Encoder[BsDiffRequest] = deriveEncoder
  implicit val bsDiffRequestIdDecoder: Decoder[BsDiffRequest] = deriveDecoder

  implicit val generatedBsDiffEncoder: Encoder[GeneratedBsDiff] = deriveEncoder
  implicit val generatedBsDiffDecoder: Decoder[GeneratedBsDiff] = deriveDecoder

  implicit val deltaGenerationFailedEncoder: Encoder[DeltaGenerationFailed] = deriveEncoder
  implicit val deltaGenerationFailedDecoder: Decoder[DeltaGenerationFailed] = deriveDecoder

  implicit val bsDiffGenerationFailedEncoder: Encoder[BsDiffGenerationFailed] = deriveEncoder
  implicit val bsDiffGenerationFailedDecoder: Decoder[BsDiffGenerationFailed] = deriveDecoder

  implicit val operationResultEncoder: Encoder[OperationResult] = deriveEncoder
  implicit val operationResultDecoder: Decoder[OperationResult] = deriveDecoder

  implicit val installationResultEncoder: Encoder[InstallationResult] = deriveEncoder
  implicit val installationResultDecoder: Decoder[InstallationResult] = deriveDecoder

  implicit val ecuInstallationReportEncoder: Encoder[EcuInstallationReport] = deriveEncoder
  implicit val ecuInstallationReportDecoder: Decoder[EcuInstallationReport] = deriveDecoder

  implicit val deviceInstallationReportEncoder: Encoder[DeviceInstallationReport] = deriveEncoder
  implicit val deviceInstallationReportDecoder: Decoder[DeviceInstallationReport] = deriveDecoder

  @deprecated("use data type from libtuf-server", "v0.1.1-21")
  implicit val deviceUpdateReportEncoder: Encoder[DeviceUpdateReport] = deriveEncoder

  implicit val updateTypeEncoder: Encoder[UpdateType] = Encoder.enumEncoder(UpdateType)
  implicit val updateTypeDecoder: Decoder[UpdateType] = Decoder.enumDecoder(UpdateType)

  // For backwards compatibility reasons we have a decoder that can parse DeviceUpdateReport
  // without a statusCode.
  @deprecated("use data type from libtuf-server", "v0.1.1-21")
  implicit val deviceUpdateReportDecoder: Decoder[DeviceUpdateReport] = Decoder.instance { cursor =>
    for {
      namespace <- cursor.downField("namespace").as[Namespace]
      device <- cursor.downField("device").as[DeviceId]
      updateId <- cursor.downField("updateId").as[UpdateId]
      timestampVersion <- cursor.downField("timestampVersion").as[Int]
      operationResult <- cursor.downField("operationResult").as[Map[EcuIdentifier, OperationResult]]
      op_resultCode <- cursor.downField("resultCode").as[Option[Int]]
      resultCode = op_resultCode.getOrElse(if (operationResult.forall(_._2.isSuccess)) 0 else 19)
    } yield DeviceUpdateReport(namespace, device, updateId, timestampVersion, operationResult, resultCode)
  }
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

  final case class TreehubCommit(ns: Namespace,
                                 commit: Commit,
                                 refName: String,
                                 description: String,
                                 size: Int,
                                 uri: String)

  final case class DeviceEventMessage(namespace: Namespace, event: Event)

  case class BandwidthUsage(id: UUID, namespace: Namespace, timestamp: Instant, byteCount: Long,
                            updateType: UpdateType, updateId: String)

  case class ImageStorageUsage(namespace: Namespace, timestamp: Instant, byteCount: Long)

  final case class DeviceUpdateEvent(namespace: Namespace,
                                     eventTime: Instant,
                                     correlationId: CorrelationId,
                                     deviceUuid: DeviceId,
                                     updateStatus: DeviceUpdateStatus)

  @deprecated("use data type from libtuf_server", "v0.1.1-21")
  case class DeviceUpdateReport(namespace: Namespace, device: DeviceId, updateId: UpdateId, timestampVersion: Int,
                                operationResult: Map[EcuIdentifier, OperationResult], resultCode: Int)

  final case class DeviceInstallationReport(namespace: Namespace, device: DeviceId,
                                            correlationId: CorrelationId,
                                            result: InstallationResult,
                                            ecuReports: Map[EcuIdentifier, EcuInstallationReport],
                                            report: Option[Json],
                                            receivedAt: Instant)

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

  implicit val treeHubCommitMessageLike = MessageLike.derive[TreehubCommit](_.commit.value)

  implicit val deviceEventMessageType = MessageLike[DeviceEventMessage](_.namespace.get)

  implicit val deviceUpdateEventType = MessageLike[DeviceUpdateEvent](_.namespace.get)

  @deprecated("use data type from libtuf_server", "v0.1.1-21")
  implicit val deviceUpdateReportMessageLike = MessageLike[DeviceUpdateReport](_.device.toString)

  implicit val deviceInstallationReportMessageLike = MessageLike[DeviceInstallationReport](_.device.toString)
}
