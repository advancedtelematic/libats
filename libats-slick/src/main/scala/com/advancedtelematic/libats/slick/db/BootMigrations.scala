/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
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

  def apply(config: Config): Try[Int] = Try {
    _log.info("Running migrations")

    val url = config.getString("database.url")
    val user = config.getString("database.properties.user")
    val password = config.getString("database.properties.password")
    val schemaO = Either.catchOnly[ConfigException.Missing](config.getString("database.catalog")).toOption

    val flyway = new Flyway
    flyway.setDataSource(url, user, password)

    schemaO.foreach { schema =>
      flyway.setSchemas(schema)
    }

    val count = flyway.migrate()
    _log.info(s"Ran $count migrations")

    count
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
