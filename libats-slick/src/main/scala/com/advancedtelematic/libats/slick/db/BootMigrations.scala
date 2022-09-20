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

package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.http.BootApp
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import cats.implicits._

protected [db] object RunMigrations {
  private lazy val _log = LoggerFactory.getLogger(this.getClass)

  def schemaIsCompatible(config: Config): Try[Boolean] = Try {
    val f = flyway(config)
    val pendingCount = f.info().pending().length

    if(pendingCount > 0) {
      _log.error(s"$pendingCount migrations pending")
      false
    } else
      true
  }

  def apply(config: Config): Try[Int] = Try {
    _log.info("Running migrations")

    val f = flyway(config)

    val count = f.migrate()
    _log.info(s"Ran $count migrations")

    count
  }

  private def flyway(config: Config): Flyway = {
    val url = config.getString("database.url")
    val user = config.getString("database.properties.user")
    val password = config.getString("database.properties.password")
    val schemaO = Either.catchOnly[ConfigException.Missing](config.getString("database.catalog")).toOption
    val schemaTable = Either.catchOnly[ConfigException.Missing](
      config.getString("database.schema-table")
    ).toOption.getOrElse(
      ConfigFactory.load().getString("ats.database.schema-table")
    )

    val ignoreMissingMigrations =
      Either.catchOnly[ConfigException.Missing](config.getBoolean("database.flyway.ignoreMissingMigrations"))
        .fold(_ => false, identity)

    val flywayConfig =
      Flyway
        .configure()
        .dataSource(url, user, password)
        .table(schemaTable)
        .ignoreMissingMigrations(ignoreMissingMigrations)

    schemaO.fold(flywayConfig)(s => flywayConfig.schemas(s)).load()
  }
}

object RunMigrationsApp extends App {
  private val log = LoggerFactory.getLogger(this.getClass)
  lazy val config = ConfigFactory.load()

  RunMigrations(config) match {
    case Success(_) =>
      System.exit(0)
    case Failure(ex) =>
      log.error("Could not run migrations", ex)
      System.exit(1)
  }
}

trait CheckMigrations {
  self: BootApp =>

  if(!config.getBoolean("ats.database.skip_migration_check")) {
    RunMigrations.schemaIsCompatible(config) match {
      case Success(false) =>
        log.error("Outdated migrations, terminating")
        system.terminate()
      case Success(true) =>
        log.info("Schema is up to date")
      case Failure(ex) =>
        log.error("Could not check schema changes compatibility", ex)
        system.terminate()
    }
  } else
    log.info("Skipping schema compatibility check due to configuration")
}

trait BootMigrations {
  self: BootApp =>

  private def migrateIfEnabled: Future[Unit] = Future {
    if (config.getBoolean("database.migrate")) {
      RunMigrations(config)
    }
  }

  if(config.getBoolean("ats.database.asyncMigrations")) {
    migrateIfEnabled.onComplete {
      case Success(_) =>
        log.info("Finished running migrations")
      case Failure(ex) =>
        log.error("Could not run migrations. Fatal error, shutting down", ex)
        system.terminate()
    }
  } else {
    Await.result(migrateIfEnabled, Duration.Inf)
  }
}
