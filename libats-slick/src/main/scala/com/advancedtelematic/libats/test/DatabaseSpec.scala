/*
 * Copyright (C) 2016 HERE Global B.V.
 *
 * Licensed under the Mozilla Public License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.mozilla.org/en-US/MPL/2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: MPL-2.0
 * License-Filename: LICENSE
 */

package com.advancedtelematic.libats.test

import java.util.TimeZone

import com.typesafe.config.{Config, ConfigFactory}
import org.flywaydb.core.Flyway
import org.scalatest.{BeforeAndAfterAll, Suite}
import slick.jdbc.MySQLProfile.api._

import scala.collection.JavaConverters._

trait DatabaseSpec extends BeforeAndAfterAll {
  self: Suite =>

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  implicit lazy val db = Database.forConfig("", slickDbConfig)

  protected lazy val schemaName = {
    val catalog = testDbConfig.getString("catalog")
    val className = this.getClass.getSimpleName
    val cleanSchemaName = catalog.split(",").head
    cleanSchemaName + "_" + className
  }

  private lazy val config = ConfigFactory.load()

  private lazy val testDbConfig: Config = config.getConfig("database")

  private [libats] lazy val slickDbConfig: Config = {
    val withSchemaName =
      ConfigFactory.parseMap(Map("catalog" -> schemaName.toLowerCase).asJava)
    withSchemaName.withFallback(testDbConfig)
  }

  protected [libats] def cleanDatabase(): Unit = {
    flyway.clean()
  }

  private lazy val flyway = {
    val url = slickDbConfig.getString("url")
    val user = slickDbConfig.getConfig("properties").getString("user")
    val password = slickDbConfig.getConfig("properties").getString("password")

    val schemaName = slickDbConfig.getString("catalog")

    Flyway.configure()
      .dataSource(url, user, password)
      .schemas(schemaName)
      .locations("classpath:db.migration")
      .load()
  }

  private def resetDatabase() = {
    flyway.clean()
    flyway.migrate()
  }

  override def beforeAll() {
    resetDatabase()
    super.beforeAll()
  }

  override def afterAll() {
    db.close()
    super.afterAll()
  }
}
