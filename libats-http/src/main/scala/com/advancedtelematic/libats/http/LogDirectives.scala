/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.http

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directive0, Directives}
import akka.stream.Materializer
import com.advancedtelematic.libats.http.logging.RequestLoggingActor

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

  private def formatResponseLog(metrics: Map[String, String]): String = {
    metrics.toList.map { case (m, v) => s"$m=$v"}.mkString(" ")
  }
}
