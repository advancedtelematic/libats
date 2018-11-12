package com.advancedtelematic.libats.data

import java.util.UUID
import io.circe.{Decoder, Encoder}
import scala.util.control.NoStackTrace

object Urn {

  abstract class Urn {
    val nid: String
    val nss: String
    override def toString = s"urn:$nid:$nss"
  }

  case class IllegalUrnException(val msg: String) extends Exception(msg) with NoStackTrace

  final case class CorrelationId(resource: String, id: UUID) extends Urn {
    override val nid = CorrelationId.namespace
    override val nss = s"$resource:${id.toString}"
  }

  object CorrelationId {
    val namespace = "here-ota"
    def fromString(value: String) = value.split(":") match {
      case Array("urn", `namespace`, resource, id) => CorrelationId(resource, UUID.fromString(id))
      case _ => throw IllegalUrnException(value)
    }

    implicit val correlationIdEncoder: Encoder[CorrelationId] = Encoder[String].contramap(_.toString)
    implicit val correlationIdDecoder: Decoder[CorrelationId] = Decoder[String].map(CorrelationId.fromString(_))
  }

}
