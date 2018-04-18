
package com.advancedtelematic.libats.messaging_datatype

import java.net.URI
import java.time.Instant
import java.util.UUID

import cats.syntax.either._
import com.advancedtelematic.libats.data.DataType.{Checksum, Namespace}
import com.advancedtelematic.libats.data.RefinedUtils._
import com.advancedtelematic.libats.messaging_datatype.DataType.UpdateType.UpdateType
import com.advancedtelematic.libats.messaging_datatype.DataType._
import com.advancedtelematic.libats.messaging_datatype.Messages.{BsDiffGenerationFailed, BsDiffRequest, CampaignLaunched, DeltaGenerationFailed, DeltaRequest, DeviceUpdateReport, GeneratedBsDiff, GeneratedDelta, UserCreated}
import io.circe._
import io.circe.generic.semiauto._

object MessageCodecs {
  import com.advancedtelematic.libats.codecs.CirceCodecs._

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

  implicit val keyDecoderEcuSerial: KeyDecoder[EcuSerial] = KeyDecoder.instance { value =>
    value.refineTry[ValidEcuSerial].toOption
  }
  implicit val keyEncoderEcuSerial: KeyEncoder[EcuSerial] = KeyEncoder[String].contramap(_.value)

  implicit val operationResultEncoder: Encoder[OperationResult] = deriveEncoder
  implicit val operationResultDecoder: Decoder[OperationResult] = deriveDecoder

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
      operationResult <- cursor.downField("operationResult").as[Map[EcuSerial, OperationResult]]
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

  case class BandwidthUsage(id: UUID, namespace: Namespace, timestamp: Instant, byteCount: Long,
                            updateType: UpdateType, updateId: String)

  case class ImageStorageUsage(namespace: Namespace, timestamp: Instant, byteCount: Long)

  @deprecated("use data type from libtuf_server", "v0.1.1-21")
  case class DeviceUpdateReport(namespace: Namespace, device: DeviceId, updateId: UpdateId, timestampVersion: Int,
                                operationResult: Map[EcuSerial, OperationResult], resultCode: Int)

  implicit val userCreatedMessageLike = MessageLike[UserCreated](_.id)

  implicit val deviceSeenMessageLike = MessageLike[DeviceSeen](_.uuid.toString)

  implicit val campaignLaunchedMessageLike = MessageLike[CampaignLaunched](_.updateId.toString)

  implicit val staticDeltaRequestMessageLike = MessageLike[DeltaRequest](_.id.uuid.toString)

  implicit val bsDiffRequestMessageLike = MessageLike[BsDiffRequest](_.id.uuid.toString)

  implicit val staticDeltaResponseMessageLike = MessageLike[GeneratedDelta](_.id.uuid.toString)

  implicit val generatedBsDiffMessageLike = MessageLike[GeneratedBsDiff](_.id.uuid.toString)

  implicit val bsDiffGenerationFailedMessageLike = MessageLike[BsDiffGenerationFailed](_.id.toString)

  implicit val deltaGenerationFailedMessageLike = MessageLike[DeltaGenerationFailed](_.id.uuid.toString)

  implicit val bandwidthUsageMessageLike = MessageLike[BandwidthUsage](_.id.toString)

  implicit val imageStorageMessageLike = MessageLike[ImageStorageUsage](_.namespace.get)

  implicit val treeHubCommitMessageLike = MessageLike[TreehubCommit](_.commit.value)

  @deprecated("use data type from libtuf_server", "v0.1.1-21")
  implicit val deviceUpdateReportMessageLike = MessageLike[DeviceUpdateReport](_.device.toString)
}
