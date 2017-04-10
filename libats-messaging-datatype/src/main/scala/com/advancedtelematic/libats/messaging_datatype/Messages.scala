
package com.advancedtelematic.libats.messaging_datatype

import java.time.Instant
import java.util.{Base64, UUID}

import akka.http.scaladsl.model.Uri
import com.advancedtelematic.libats.data.Namespace
import com.advancedtelematic.libats.messaging.Messages.MessageLike
import com.advancedtelematic.libats.messaging_datatype.DataType.UpdateType.UpdateType
import com.advancedtelematic.libats.messaging_datatype.DataType.{Commit, DeltaRequestId, PackageId}
import com.advancedtelematic.libats.messaging_datatype.Messages.{CampaignLaunched, DeltaRequest, GeneratedDelta, UserCreated}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto._

object MessageCodecs {
  import com.advancedtelematic.libats.codecs.AkkaCirce._
  import com.advancedtelematic.libats.codecs.Codecs.{namespaceEncoder, namespaceDecoder}

  implicit val javaUuidEncoder : Encoder[UUID] = Encoder[String].contramap(_.toString)
  implicit val javaUuidDecoder : Decoder[UUID] = Decoder[String].map(UUID.fromString)

  implicit val uriEncoder : Encoder[Uri] = Encoder[String].contramap(_.toString)
  implicit val uriDecoder : Decoder[Uri] = Decoder[String].map(Uri.apply)

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
}

object Messages {
  import MessageCodecs._
  import com.advancedtelematic.libats.codecs.AkkaCirce._
  import com.advancedtelematic.libats.codecs.Codecs.{namespaceEncoder, namespaceDecoder}

  final case class UserCreated(id: String)

  final case class CampaignLaunched(namespace: String, updateId: UUID,
                                    devices: Set[UUID], pkgUri: Uri,
                                    pkg: PackageId, pkgSize: Long, pkgChecksum: String)

  case class DeltaRequest(id: DeltaRequestId, namespace: Namespace, from: Commit, to: Commit, timestamp: Instant = Instant.now)

  case class GeneratedDelta(id: DeltaRequestId, namespace: Namespace, from: Commit, to: Commit, uri: Uri, size: Long)

  case class DeltaGenerationFailed(id: DeltaRequestId, namespace: Namespace, error: Option[Json] = None)

  final case class TreehubCommit(ns: Namespace,
                                 commit: Commit,
                                 refName: String,
                                 description: String,
                                 size: Int,
                                 uri: String)

  case class BandwidthUsage(id: UUID, namespace: Namespace, timestamp: Instant, byteCount: Long,
                            updateType: UpdateType, updateId: String)

  case class ImageStorageUsage(namespace: Namespace, timestamp: Instant, byteCount: Long)

  implicit val userCreatedMessageLike = MessageLike[UserCreated](_.id)

  implicit val campaignLaunchedMessageLike = MessageLike[CampaignLaunched](_.updateId.toString)

  implicit val staticDeltaRequestMessageLike = MessageLike[DeltaRequest](_.id.uuid.toString)

  implicit val staticDeltaResponseMessageLike = MessageLike[GeneratedDelta](_.id.uuid.toString)

  implicit val deltaGenerationFailedMessageLike = MessageLike[DeltaGenerationFailed](_.id.uuid.toString)

  implicit val bandwidthUsageMessageLike = MessageLike[BandwidthUsage](_.id.toString)

  implicit val imageStorageMessageLike = MessageLike[ImageStorageUsage](_.namespace.get)

  implicit val treeHubCommitMessageLike = MessageLike[TreehubCommit](_.commit.get)
}