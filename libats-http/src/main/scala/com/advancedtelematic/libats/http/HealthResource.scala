/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.http


import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import com.advancedtelematic.libats.codecs.AkkaCirce._
import com.advancedtelematic.libats.http.monitoring.{JvmMetrics, LoggerMetrics, MetricsSupport}
import com.codahale.metrics.MetricRegistry
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}

trait HealthCheck {
  def apply(logger: LoggingAdapter)(implicit ec: ExecutionContext): Future[Unit]
}

trait HealthMetrics {
  def metricsJson: Future[Json]

  def urlPrefix: String
}

class HealthResource(versionRepr: Map[String, Any] = Map.empty,
                     healthChecks: Seq[HealthCheck] = Seq.empty,
                     healthMetrics: Seq[HealthMetrics] = Seq.empty,
                     metricRegistry: MetricRegistry = MetricsSupport.metricRegistry
                    )(implicit val ec: ExecutionContext) {
  import Directives._

  val defaultMetrics = Seq(new JvmMetrics(metricRegistry), new LoggerMetrics(metricRegistry))

  def route = {
      (get & pathPrefix("health") & extractLog) { logger =>

        val healthRoutes =
          pathEnd {
            val f = Future.sequence(healthChecks.map(_ (logger))).map(_ => StatusCodes.OK -> Map("status" -> "OK"))
              .recover { case _ => StatusCodes.ServiceUnavailable -> Map("status" -> "DOWN")}
            complete(f)
          } ~
          path("version") {
            complete(versionRepr.mapValues(_.toString).asJson)
          }

        (defaultMetrics ++ healthMetrics).foldLeft(healthRoutes) { (routes, metricSet) =>
          routes ~ path(metricSet.urlPrefix) {
            complete(metricSet.metricsJson)
          }
        }
      } ~ LoggingResource.route
  }
}
