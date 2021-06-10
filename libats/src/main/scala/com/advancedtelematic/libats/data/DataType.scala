package com.advancedtelematic.libats.data

import cats.Show

import java.util.UUID
import com.advancedtelematic.libats.data.DataType.HashMethod.HashMethod
import eu.timepit.refined.api.{Refined, Validate}
import io.circe.{Decoder, Encoder}

import scala.language.postfixOps

object DataType {

  // The underlying type is String instead of UUID because we need to support the legacy format of the namespaces
  final case class Namespace(get: String) extends AnyVal

  object Namespace {
    def generate: Namespace = new Namespace(s"urn:here-ota:namespace:${UUID.randomUUID().toString}")
  }

  final case class ResultCode(value: String) extends AnyVal
  final case class ResultDescription(value: String) extends AnyVal

  case class Checksum(method: HashMethod, hash: Refined[String, ValidChecksum])

  object HashMethod extends Enumeration {
    type HashMethod = Value

    val SHA256 = Value("sha256")
  }

  case class ValidChecksum()

  implicit val validChecksum: Validate.Plain[String, ValidChecksum] =
    ValidationUtils.validHexValidation(ValidChecksum(), length = 64)

  sealed trait CorrelationId
  final case class CorrelationCampaignId(value: UUID) extends CorrelationId {
    override def toString: String = s"urn:here-ota:campaign:$value"
  }

  final case class MultiTargetUpdateId(value: UUID) extends CorrelationId {
    override def toString: String = s"urn:here-ota:mtu:$value"
  }

  final case class AutoUpdateId(value: UUID) extends CorrelationId {
    override def toString: String = s"urn:here-ota:auto-update:$value"
  }

  object CorrelationId {
    private[this] val CorrelationIdRe = """^urn:here-ota:(mtu|auto-update|campaign):([0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12})$"""r

    def fromString(s: String): Either[String, CorrelationId] = s match {
      case CorrelationIdRe("mtu", uuid) =>
        Right(MultiTargetUpdateId(UUID.fromString(uuid)))
      case CorrelationIdRe("campaign", uuid) =>
        Right(CorrelationCampaignId(UUID.fromString(uuid)))
      case CorrelationIdRe("auto-update", uuid) =>
        Right(AutoUpdateId(UUID.fromString(uuid)))
      case x =>
        Left(s"Invalid correlationId: '$x'.")
    }

    implicit val DecoderInstance: Decoder[CorrelationId] = Decoder.decodeString.emap(CorrelationId.fromString)
    implicit val EncoderInstance: Encoder[CorrelationId] = Encoder.encodeString.contramap(_.toString)
  }
}
