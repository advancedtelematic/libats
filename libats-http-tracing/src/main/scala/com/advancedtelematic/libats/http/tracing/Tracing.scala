package com.advancedtelematic.libats.http.tracing

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directive1
import com.typesafe.config.{Config, ConfigException}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

object Tracing {
  private lazy val _log = LoggerFactory.getLogger(this.getClass)

  trait Tracing {
    def traceRequests: Directive1[ServerRequestTracing]

    def shutdown(): Unit
  }

  trait AkkaHttpClientTracing {
    def trace(fn: HttpRequest => Future[HttpResponse])(implicit ec: ExecutionContext): HttpRequest => Future[HttpResponse]
  }

  trait ServerRequestTracing {
    def traceId: Long

    def newChild: ServerRequestTracing

    def finishSpan(): Unit

    def httpClientTracing(remoteServiceName: String): AkkaHttpClientTracing
  }

  def fromConfig(config: Config, serviceName: String): Tracing =
    try {
      if (config.getBoolean("ats.http.tracing.enabled")) {
        val uri = Uri(config.getString("ats.http.tracing.zipkin_uri"))
        _log.info(s"zipkin tracing enabled to $uri")
        ZipkinServerRequestTracing(uri, serviceName)
      } else {
        _log.info("Request tracing disabled in config")
        new NullTracing
      }
    } catch {
      case _: ConfigException.Missing =>
        _log.warn("Request tracing disabled, zipkin configuration missing")
        new NullTracing
    }
}
