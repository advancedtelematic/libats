package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.test.DatabaseSpec
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FunSuite, Matchers}
import slick.jdbc.MySQLProfile.api._

class RunMigrationsSpec extends FunSuite with Matchers with ScalaFutures with DatabaseSpec {

  override implicit def patienceConfig = PatienceConfig().copy(timeout = Span(5, Seconds))

  lazy val flywayConfig = slickDbConfig.atKey("database")

  test("runs migrations") {
    cleanDatabase()

    RunMigrations(flywayConfig).get shouldBe 1

    val sql = sql"select count(*) from schema_version".as[Int]
    db.run(sql).futureValue.head shouldBe > (1)
  }

  test("reports pending migrations") {
    cleanDatabase()
    RunMigrations.schemaIsCompatible(flywayConfig).get shouldBe false
  }

  test("runs without pending migrations") {
    cleanDatabase()
    RunMigrations(flywayConfig).get shouldBe 1
    RunMigrations.schemaIsCompatible(flywayConfig).get shouldBe true
  }

  override protected def testDbConfig: Config = ConfigFactory.load().getConfig("ats.database")
}
