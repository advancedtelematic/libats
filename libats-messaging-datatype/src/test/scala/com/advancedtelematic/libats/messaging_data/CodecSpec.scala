package com.advancedtelematic.libats.messaging_data

import cats.syntax.show._
import com.advancedtelematic.libats.data.DataType.{HashMethod, Namespace, ValidChecksum}
import com.advancedtelematic.libats.data.{EcuIdentifier, SmartStringConstructor}
import com.advancedtelematic.libats.data.RefinedUtils._
import com.advancedtelematic.libats.messaging_datatype.DataType._
import com.advancedtelematic.libats.messaging_datatype.MessageCodecs._
import com.advancedtelematic.libats.messaging_datatype.Messages._
import io.circe.syntax._
import io.circe.parser.decode
import org.scalatest.{FunSuite, Matchers}

class CodecSpec extends FunSuite with Matchers {

  val targetA = "target-a".refineTry[ValidTargetFilename].get
  val hashA = Map(HashMethod.SHA256 -> "0a79a6bef63c2205d4c6dac360b4b4600366f695bb18b5e3eec4e2297df50666".refineTry[ValidChecksum].get)

  val operationResultOk =
    OperationResult(targetA, hashA, 22, 0, "OK")

  val operationResultFailed =
    OperationResult(targetA, hashA, 22, 19, "Failed")

  val ecu1 = SmartStringConstructor[EcuIdentifier]("ecu1").right.get
  val ecu2 = SmartStringConstructor[EcuIdentifier]("ecu2").right.get

  val device = DeviceId.generate
  val update = UpdateId.generate

  val deviceUpdateReportOk =
    DeviceUpdateReport(Namespace("ns"), device, update, 1,
                       Map(ecu1 -> operationResultOk, ecu2 -> operationResultOk),
                       0)

  val deviceUpdateReportFailed =
    DeviceUpdateReport(Namespace("ns"), device, update, 1,
                       Map(ecu1 -> operationResultOk, ecu2 -> operationResultFailed),
                       19)

  test("encode/decode for DeviceUpdateReport") {
    decode[DeviceUpdateReport](deviceUpdateReportOk.asJson.noSpaces) shouldBe Right(deviceUpdateReportOk)
    decode[DeviceUpdateReport](deviceUpdateReportFailed.asJson.noSpaces) shouldBe Right(deviceUpdateReportFailed)
  }

  test("decode DeviceUpdateReport without statusCode") {
    val opOkay = operationResultOk.asJson.noSpaces
    val opFail = operationResultFailed.asJson.noSpaces

    val ok = s"""{"namespace": "ns", "device": "${device.show}", "updateId": "${update.show}", "timestampVersion": 1, "operationResult": {"ecu1": $opOkay, "ecu2": $opOkay}}"""

    val failed = s"""{"namespace": "ns", "device": "${device.show}", "updateId": "${update.show}", "timestampVersion": 1, "operationResult": {"ecu1": $opOkay, "ecu2": $opFail}}"""

    decode[DeviceUpdateReport](ok) shouldBe Right(deviceUpdateReportOk)
    decode[DeviceUpdateReport](failed) shouldBe Right(deviceUpdateReportFailed)
  }
}
