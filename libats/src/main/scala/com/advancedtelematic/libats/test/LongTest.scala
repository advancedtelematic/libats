package com.advancedtelematic.libats.test

import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Seconds, Span}

trait LongTest extends PatienceConfiguration {
  override implicit def patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(100, Millis))
}
