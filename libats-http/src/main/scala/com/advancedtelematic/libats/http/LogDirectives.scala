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

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directive0, Directives}
import akka.stream.Materializer
import ch.qos.logback.classic.LoggerContext
import com.advancedtelematic.libats.http.logging.RequestLoggingActor
import org.slf4j.LoggerFactory

import scala.util.Try

object LogDirectives {
  import Directives._

  type MetricsBuilder = (HttpRequest, HttpResponse) => Map[String, String]

  lazy val envServiceName = sys.env.get("SERVICE_NAME")

  def logResponseMetrics(defaultServiceName: String,
                         extraMetrics: MetricsBuilder = (_, _) => Map.empty,
                         level: LogLevel = Logging.InfoLevel)
                        (implicit system: ActorSystem): Directive0 = {

    val serviceName = envServiceName.getOrElse(defaultServiceName)
    val requestLoggingActorRef = system.actorOf(RequestLoggingActor.router(level), "request-log-router")

    extractRequestContext.flatMap { ctx =>
      val startAt = System.currentTimeMillis()
      val namespace = ctx.request.headers.find(_.is("x-ats-namespace")).map("req_namespace" -> _.value()).toMap

      mapResponse { resp =>
        val responseTime = System.currentTimeMillis() - startAt
        val allMetrics =
          defaultMetrics(ctx.request, resp, responseTime, serviceName) ++ extraMetrics(ctx.request, resp) ++ namespace

        requestLoggingActorRef ! RequestLoggingActor.LogMsg(formatResponseLog(allMetrics), allMetrics)

        resp
      }
    }
  }

  private def defaultMetrics(request: HttpRequest, response: HttpResponse, serviceTime: Long, serviceName: String): Map[String, String] = {
    Map(
      "http_method" -> request.method.name,
      "http_path" -> request.uri.path.toString,
      "http_query" -> s"'${request.uri.rawQueryString.getOrElse("").toString}'",
      "http_stime" -> serviceTime.toString,
      "http_status" -> response.status.intValue.toString,
      "http_service_name" -> serviceName
    )
  }

  private lazy val usingJsonAppender = {
    import scala.collection.JavaConverters._
    val loggers = Try(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]).toOption.toList.flatMap(_.getLoggerList.asScala)
    loggers.exists(_.iteratorForAppenders().asScala.exists(_.getName.contains("json")))
  }

  private def formatResponseLog(metrics: Map[String, String]): String = {
    if (usingJsonAppender)
      "http request" // `metrics` will be logged in json mdc context, see com.advancedtelematic.libats.logging.JsonEncoder
    else
      metrics.toList.map { case (m, v) => s"$m=$v"}.mkString(" ")
  }
}
