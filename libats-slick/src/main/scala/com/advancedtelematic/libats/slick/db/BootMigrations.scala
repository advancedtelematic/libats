/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.http.BootApp
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

trait AsyncMigrations {
  self: BootApp =>

  private val _migrateLog = LoggerFactory.getLogger(this.getClass)

  def migrate: Future[Int] = Future {
    if (config.getBoolean("database.migrate")) {
      _migrateLog.info("Running migrations")

      val url = config.getString("database.url")
      val user = config.getString("database.properties.user")
      val password = config.getString("database.properties.password")

      val flyway = new Flyway
      flyway.setDataSource(url, user, password)

      val count = flyway.migrate()
      _migrateLog.info(s"Ran $count migrations")

      count
    } else
      0
  }
}

trait BootMigrations extends AsyncMigrations {
  self: BootApp =>

  if(config.getBoolean("ats.database.asyncMigrations")) {
    migrate.onComplete {
      case Success(_) =>
        log.info("Finished running migrations")
      case Failure(ex) =>
        log.error("Could not run migrations. Fatal error, shutting down", ex)
        system.terminate()
    }
  } else {
    Await.result(migrate, Duration.Inf)
  }
}
