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

package com.advancedtelematic.libats.http

import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives
import com.advancedtelematic.libats.codecs.CirceCodecs._
import com.advancedtelematic.libats.http.HealthCheck.HealthCheckResult
import com.advancedtelematic.metrics.{JvmMetrics, LoggerMetrics, MetricsRepresentation, MetricsSupport}
import com.codahale.metrics.MetricRegistry
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.{Encoder, Json}
import io.circe.syntax._
import io.circe.generic.auto._

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


class HealthResource(versionRepr: Map[String, Any] = Map.empty,
                     healthChecks: Seq[HealthCheck] = Seq.empty,
                     healthMetrics: Seq[MetricsRepresentation] = Seq.empty,
                     dependencies: Seq[HealthCheck] = Seq.empty,
                     metricRegistry: MetricRegistry = MetricsSupport.metricRegistry
                    )(implicit val ec: ExecutionContext) {
  import Directives._
  import HealthCheck._

  val defaultMetrics = Seq(new JvmMetrics(metricRegistry), new LoggerMetrics(metricRegistry))

  private def runHealthChecks(logger: LoggingAdapter, checks: Seq[HealthCheck], errorCode: StatusCode): Future[ToResponseMarshallable] = {
    val healthCheckResults = Future.traverse(checks) { check =>
      check(logger)
        .map(check.name -> _)
        .recover { case ex => check.name -> Down(ex) }
    }

    import FailFastCirceSupport._

    healthCheckResults.map { results =>
      if (results.forall(_._2 == Up))
        ToResponseMarshallable(StatusCodes.OK -> Map("status" -> "OK"))
      else
        ToResponseMarshallable(errorCode -> results.toMap)
    }.recover { case t =>
      logger.error(t, "Could not run health checks")
      StatusCodes.ServiceUnavailable -> Map("status" -> "DOWN")
    }
  }

  def route = {
      (get & pathPrefix("health") & extractLog) { logger =>
        val healthRoutes =
          pathEnd {
            val f = runHealthChecks(logger, healthChecks, StatusCodes.ServiceUnavailable)
            complete(f)
          } ~
          path("version") {
            complete(versionRepr.mapValues(_.toString).asJson)
          } ~
          path("dependencies") {
            val f = runHealthChecks(logger, dependencies, StatusCodes.BadGateway)
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
