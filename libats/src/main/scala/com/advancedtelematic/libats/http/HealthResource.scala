/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.http


import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives
import com.advancedtelematic.libats.monitoring.MetricsSupport

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import com.advancedtelematic.libats.codecs.AkkaCirce._
import de.heikoseeberger.akkahttpcirce.CirceSupport._
import io.circe.syntax._

trait HealthCheck {
  def apply(logger: LoggingAdapter)(implicit ec: ExecutionContext): Future[Unit]
}

class HealthResource(healthChecks: Seq[HealthCheck], versionRepr: Map[String, Any] = Map.empty)
                    (implicit val ec: ExecutionContext) {
  import Directives._

  val metricRegistry = MetricsSupport.metricRegistry

  def route =
    (get & pathPrefix("health") & extractLog) { logger =>
      pathEnd {
        val f = Future.sequence(healthChecks.map(_(logger))).map(_ => Map("status" -> "OK"))
        complete(f)
      } ~
        path("version") {
        complete(versionRepr.mapValues(_.toString).asJson)
      } ~
      path("jvm") {
        val jvm = metricRegistry.getGauges(MetricsSupport.JvmFilter).asScala
        val data = jvm.mapValues(_.getValue.toString)

        complete(data.toMap.asJson)
      }
    }
}
