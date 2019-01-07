package com.advancedtelematic.libats.data

import java.util.UUID

import com.advancedtelematic.libats.data.DataType.HashMethod.HashMethod
import eu.timepit.refined.api.{Refined, Validate}
import io.circe.{Decoder, Encoder}

import scala.language.postfixOps

object DataType {
  final case class Namespace(get: String) extends AnyVal

  case class Checksum(method: HashMethod, hash: Refined[String, ValidChecksum])

  object HashMethod extends Enumeration {
    type HashMethod = Value

    val SHA256 = Value("sha256")
  }

  case class ValidChecksum()

  implicit val validChecksum: Validate.Plain[String, ValidChecksum] =
    ValidationUtils.validHexValidation(ValidChecksum(), length = 64)

  sealed trait CorrelationId
  final case class CampaignId(value: UUID) extends CorrelationId {
    override def toString: String = s"urn:here-ota:campaign:$value"
  }
  final case class MultiTargetUpdateId(value: UUID) extends CorrelationId {
    override def toString: String = s"urn:here-ota:mtu:$value"
  }

  object CorrelationId {
    private[this] val CorrelationIdRe = """^urn:here-ota:(mtu|campaign):([0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12})$"""r

    def fromString(s: String): Either[String, CorrelationId] = s match {
      case CorrelationIdRe("mtu", uuid) =>
        Right(MultiTargetUpdateId(UUID.fromString(uuid)))
      case CorrelationIdRe("campaign", uuid) =>
        Right(CampaignId(UUID.fromString(uuid)))
      case x =>
        Left(s"Invalid correlationId: '$x'.")
    }

    implicit val DecoderInstance: Decoder[CorrelationId] = Decoder.decodeString.emap(CorrelationId.fromString)
    implicit val EncoderInstance: Encoder[CorrelationId] = Encoder.encodeString.contramap(_.toString)
  }

}
