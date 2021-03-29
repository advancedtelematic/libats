package com.advancedtelematic.libats.slick.db

import java.security.Security
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKitBase
import com.advancedtelematic.libats.slick.db.SlickEncryptionKeyChange.Result
import com.advancedtelematic.libats.test.{DatabaseSpec, LongTest}
import com.typesafe.config.{Config, ConfigFactory}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures
import slick.jdbc.MySQLProfile.api._

class SlickEncryptionKeyChangeSpec extends FunSuite
  with TestKitBase
  with Matchers
  with ScalaFutures
  with DatabaseSpec
  with BeforeAndAfter
  with LongTest {

  Security.addProvider(new BouncyCastleProvider)

  implicit lazy val system = ActorSystem(this.getClass.getSimpleName)

  override protected def testDbConfig: Config = ConfigFactory.load().getConfig("ats.database")

  import system.dispatcher

  val oldSalt = "zP4TEcmyaZw="
  val oldPass = "H83tIxLhILdshamQFxqULXlkLKF1ytrowuBLNIHs5aFq994Y6OxVpXTJHesValH7"
  val newSalt = "80R1NXHKHeQ="
  val newPass = "WcywlQrO7NXk2dQeAtsHO3FYUdfRXfVsmTOZR9934Tf0p14JD5VYeTIGIJv27sXk"

  val otherSalt = "sl2kdNyAfAs="
  val otherPass = "prLLJ5iYgoiE3vWoKJktTWCam8984UzCkvN5U91DAuaAn5N3DMRrqdS7Yoc8bjfo"

  val oldCrypto = SlickCrypto(oldSalt, oldPass)
  val newCrypto = SlickCrypto(newSalt, newPass)
  val otherCrypto = SlickCrypto(otherSalt, otherPass)

  val tableName = "re_key_spec"

  before {
    val deleteQ = sqlu"""delete from `#$tableName`"""

    val insertQ =
      sql"""
           |insert into `#$tableName` (id, uuid, encrypted_col) values
           |(1, '07b9ff11-e47f-4b8a-9c00-49484ded75c2', ${oldCrypto.encrypt("mytext")}),
           |(2, 'b1fef8d4-a292-42b6-8ae3-36ed2cc8d3fd', ${otherCrypto.encrypt("mytext")})
        """.stripMargin.asUpdate

    db.run(deleteQ.andThen(insertQ)).futureValue
  }

  def runOn(column: String) =
    new SlickEncryptionKeyChange("uuid", tableName, "encrypted_col", oldSalt, oldPass, newSalt, newPass).run

  def findRow(idCol: String, id: String) = {
    val q = sql"select encrypted_col from #$tableName where `#$idCol` = $id".as[String]
    db.run(q.map(_.head))
  }

  test("can re encrypt value with new key using uuid pk") {
    val result = runOn("uuid").futureValue
    result shouldBe Result(1, 1)

    val newEncryptedValue = findRow("uuid", "07b9ff11-e47f-4b8a-9c00-49484ded75c2").futureValue
    newCrypto.decrypt(newEncryptedValue) shouldBe "mytext"
  }

  test("leaves error columns unchanged") {
    val result = runOn("id").futureValue
    result shouldBe Result(1, 1)

    val newEncryptedValue = findRow("id", "2").futureValue
    otherCrypto.decrypt(newEncryptedValue) shouldBe "mytext"
  }

  test("can re encrypt value with new key using int pk") {
    val result = runOn("id").futureValue
    result shouldBe Result(1, 1)

    val newEncryptedValue = findRow("id", "1").futureValue
    newCrypto.decrypt(newEncryptedValue) shouldBe "mytext"
  }
}
