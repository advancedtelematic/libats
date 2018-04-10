/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.http


import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import com.advancedtelematic.libats.codecs.CirceCodecs._
import com.advancedtelematic.libats.http.HealthCheck.{Down, HealthCheckResult, Up}
import com.advancedtelematic.libats.http.monitoring.{JvmMetrics, LoggerMetrics, MetricsSupport}
import com.codahale.metrics.MetricRegistry
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.{Encoder, Json}
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}

object HealthCheck {
  import io.circe.syntax._

  sealed trait HealthCheckResult
  object Up extends HealthCheckResult
  case class Down(cause: Throwable) extends HealthCheckResult

  implicit val healthCheckResultEncoders = Encoder.instance[HealthCheckResult] {
    case Up => Json.obj("status" -> "up".asJson)
    case Down(cause) => Json.obj("status" -> "down".asJson, "cause" -> cause.getMessage.asJson)
  }
}

trait HealthCheck {
  def name: String

  def apply(logger: LoggingAdapter)(implicit ec: ExecutionContext): Future[HealthCheckResult]
}

trait HealthMetrics {
  def metricsJson: Future[Json]

  def urlPrefix: String
}

class HealthResource(versionRepr: Map[String, Any] = Map.empty,
                     healthChecks: Seq[HealthCheck] = Seq.empty,
                     healthMetrics: Seq[HealthMetrics] = Seq.empty,
                     dependencies: Seq[HealthCheck] = Seq.empty,
                     metricRegistry: MetricRegistry = MetricsSupport.metricRegistry
                    )(implicit val ec: ExecutionContext) {
  import Directives._
  import HealthCheck._

  val defaultMetrics = Seq(new JvmMetrics(metricRegistry), new LoggerMetrics(metricRegistry))

  def route = {
      (get & pathPrefix("health") & extractLog) { logger =>

        val healthRoutes =
          pathEnd {
            val f = Future.sequence(healthChecks.map(_ (logger))).map(_ => StatusCodes.OK -> Map("status" -> "OK"))
              .recover { case t =>
                logger.error(t, "Health check failed.")
                StatusCodes.ServiceUnavailable -> Map("status" -> "DOWN")
              }
            complete(f)
          } ~
          path("version") {
            complete(versionRepr.mapValues(_.toString).asJson)
          } ~
          path("dependencies") {
            val dependenciesChecks = Future.traverse(dependencies) { dependency =>
              dependency(logger)
                .map(dependency.name -> _)
                .recover { case ex => dependency.name -> Down(ex) }
            }

            val f = dependenciesChecks.map { res =>
              if (res.forall(_._2 == Up))
                StatusCodes.OK -> res.toMap
              else
                StatusCodes.BadGateway -> res.toMap
            }

            complete(f)
          }

        (defaultMetrics ++ healthMetrics).foldLeft(healthRoutes) { (routes, metricSet) =>
          routes ~ path(metricSet.urlPrefix) {
            complete(metricSet.metricsJson)
          }
        }
      } ~ LoggingResource.route
  }
}
