package com.advancedtelematic.libats.data

import java.util.UUID

import com.advancedtelematic.libats.data.DataType.{CampaignId, CorrelationId, MultiTargetUpdateId}
import org.scalatest.{EitherValues, Matchers, PropSpec}

class CorrelationIdSpec extends PropSpec with Matchers with EitherValues {

  property("should convert a MultiTargetUpdateId to string") {
    val uuid = "6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
    val cid = MultiTargetUpdateId(UUID.fromString(uuid))
    cid.toString shouldBe "urn:here-ota:mtu:6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
  }

  property("should convert a CampaignId to string") {
    val uuid = "6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
    val cid = CampaignId(UUID.fromString(uuid))
    cid.toString shouldBe "urn:here-ota:campaign:6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
  }

  property("should parse a string as a MultiTargetUpdateId") {
    val uuid = "6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
    val s = s"urn:here-ota:mtu:$uuid"
    val cid = CorrelationId.fromString(s)
    cid.right.value shouldBe MultiTargetUpdateId(UUID.fromString(uuid))
  }

  property("should parse a string as a CampaignId") {
    val uuid = "6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
    val s = s"urn:here-ota:campaign:$uuid"
    val cid = CorrelationId.fromString(s)
    cid.right.value shouldBe CampaignId(UUID.fromString(uuid))
  }

  property("should fail with an invalid correlationId string") {
    val uuid = "6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
    val s = s"invalid:here-ota:campaign:$uuid"
    val cid = CorrelationId.fromString(s)
    cid.left.value shouldBe s"Invalid correlationId: '$s'."
  }

}
