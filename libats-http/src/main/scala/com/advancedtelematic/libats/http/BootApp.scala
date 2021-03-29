/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package com.advancedtelematic.libats.http

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

trait BootApp {
  implicit val system: ActorSystem
  val appConfig: Config
}

trait BootAppDefaultConfig extends App {
  val projectName: String

  implicit lazy val system = ActorSystem(projectName)
  implicit lazy val exec = system.dispatcher
  lazy val log = LoggerFactory.getLogger(this.getClass)

  lazy val appConfig = ConfigFactory.load()
}

trait BootAppDatabaseConfig {
  self: BootAppDefaultConfig =>

  lazy val dbConfig = appConfig.getConfig("ats." + projectName + ".database")
}
