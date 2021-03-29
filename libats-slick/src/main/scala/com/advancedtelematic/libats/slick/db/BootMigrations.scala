/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package com.advancedtelematic.libats.slick.db


import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.http.BootApp
import com.typesafe.config.Config
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

protected [db] object RunMigrations {
  private lazy val _log = LoggerFactory.getLogger(this.getClass)

  def schemaIsCompatible(dbConfig: Config): Try[Boolean] = Try {
    val f = flyway(dbConfig)
    val pendingCount = f.info().pending().length

    if(pendingCount > 0) {
      _log.error(s"$pendingCount migrations pending")
      false
    } else
      true
  }

  def apply(dbconfig: Config): Try[Int] = Try {
    _log.info("Running migrations")

    val f = flyway(dbconfig)

    val count = f.migrate()
    _log.info(s"Ran $count migrations")

    count
  }

  private def flyway(dbConfig: Config): Flyway = {
    val url = dbConfig.getString("url")
    val user = dbConfig.getString("properties.user")
    val password = dbConfig.getString("properties.password")

    val flywayConfig = Flyway.configure().dataSource(url, user, password)


    if(dbConfig.hasPath("flyway.locations")) {
      val locations = dbConfig.getStringList("flyway.locations").asScala
      flywayConfig.locations(locations:_*)
    }

    if(dbConfig.hasPath("flyway.schema-table")) {
      flywayConfig.table(dbConfig.getString("flyway.schema-table"))
    }

    if (dbConfig.hasPath("catalog")) {
      flywayConfig.schemas(dbConfig.getString("catalog"))
    }

    flywayConfig.load()
  }
}

trait CheckMigrations {
  self: BootApp with DatabaseSupport =>

  private lazy val _log = LoggerFactory.getLogger(this.getClass)

  if(!appConfig.getBoolean("ats.database.skipMigrationCheck")) {
    RunMigrations.schemaIsCompatible(dbConfig) match {
      case Success(false) =>
        _log.error("Outdated migrations, terminating")
        system.terminate()
      case Success(true) =>
        _log.info("Schema is up to date")
      case Failure(ex) =>
        _log.error("Could not check schema changes compatibility", ex)
        system.terminate()
    }
  } else
    _log.info("Skipping schema compatibility check due to configuration")
}


trait BootMigrations {
  self: BootApp with DatabaseSupport =>

  import system.dispatcher

  private lazy val _log = LoggerFactory.getLogger(this.getClass)

  private def migrateIfEnabled: Future[Int] = {
    if (appConfig.getBoolean("ats.database.migrate"))
      Future { Future.fromTry(RunMigrations(dbConfig)) }.flatten
    else
      FastFuture.successful(0)
  }

  if(appConfig.getBoolean("ats.database.asyncMigrations")) {
    migrateIfEnabled.onComplete {
      case Success(_) =>
        _log.info("Finished running migrations")
      case Failure(ex) =>
        _log.error("Could not run migrations. Fatal error, shutting down", ex)
        system.terminate()
    }
  } else {
    Await.result(migrateIfEnabled, Duration.Inf)
  }
}
