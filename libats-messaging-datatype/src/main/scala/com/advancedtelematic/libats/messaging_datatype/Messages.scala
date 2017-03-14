package com.advancedtelematic.libats.messaging_datatype

import java.util.UUID

import akka.http.scaladsl.model.Uri
import com.advancedtelematic.libats.messaging.Messages.MessageLike
import com.advancedtelematic.libats.messaging_datatype.DataType.PackageId
import com.advancedtelematic.libats.messaging_datatype.Messages.{CampaignLaunched, UserCreated}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

object MessageCodecs {
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
}

object Messages {

  import MessageCodecs._

  final case class UserCreated(id: String)

  final case class CampaignLaunched(namespace: String, updateId: UUID,
                                    devices: Set[UUID], pkgUri: Uri,
                                    pkg: PackageId, pkgSize: Long, pkgChecksum: String)

  implicit val userCreatedMessageLike = MessageLike[UserCreated](_.id)

  implicit val campaignLaunchedMessageLike = MessageLike[CampaignLaunched](_.updateId.toString)
}
