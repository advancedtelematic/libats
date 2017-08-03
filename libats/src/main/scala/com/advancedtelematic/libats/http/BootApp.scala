/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package com.advancedtelematic.libats.http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

trait BootApp extends App {
  val projectName: String

  implicit val system = ActorSystem(projectName)
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = LoggerFactory.getLogger(this.getClass)

  lazy val config = ConfigFactory.load()
}
