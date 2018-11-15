package com.advancedtelematic.libats.data

import com.advancedtelematic.libats.data.DataType.HashMethod.HashMethod
import eu.timepit.refined.api.{Refined, Validate}
import java.util.UUID
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
  final case class CampaignId(value: UUID) extends CorrelationId
  final case class MultiTargetUpdateId(value: UUID) extends CorrelationId

  object CorrelationId {
    private[this] val CorrelationIdRe = """^urn:here-ota:(mtu|campaign):([0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12})$"""r

    implicit val DecoderInstance: Decoder[CorrelationId] = Decoder.decodeString.emap {
      case CorrelationIdRe("mtu", uuid) =>
        Right(MultiTargetUpdateId(UUID.fromString(uuid)))

      case CorrelationIdRe("campaign", uuid) =>
        Right(CampaignId(UUID.fromString(uuid)))
      case x =>
        Left(s"'$x' is an invalid CorrelationId")
    }

    implicit val EncoderInstance: Encoder[CorrelationId] = Encoder.encodeString.contramap {
      case CampaignId(x) => s"urn:here-ota:campaign:$x"
      case MultiTargetUpdateId(x) => s"urn:here-ota:mtu:$x"
    }
  }
}
