package com.advancedtelematic.libats.http.tracing

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.server.{Directive1, Directives}
import com.advancedtelematic.libats.http.tracing.Tracing.{RequestTracing, Tracing}
import com.typesafe.config.{Config, ConfigException}
import org.slf4j.LoggerFactory

object Tracing {
  private lazy val _log = LoggerFactory.getLogger(this.getClass)

  trait Tracing {
    def traceRequests: Directive1[RequestTracing]

    def shutdown(): Unit
  }

  trait RequestTracing {
    def newChild: RequestTracing

    def headers(request: HttpRequest): Map[String, String]

    def finishSpan(): Unit
  }

  def fromConfig(config: Config, serviceName: String): Tracing =
    try {
      if (config.getBoolean("ats.http.tracing.enabled")) {
        val uri = Uri(config.getString("ats.http.tracing.zipkin_uri"))
        _log.info(s"zipkin tracing enabled to $uri")
        ZipkinRequestTracing(uri, serviceName)
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

class NullRequestTracing extends RequestTracing  {
  override def newChild: RequestTracing = this

  override def headers(request: HttpRequest): Map[String, String] = Map.empty

  override def finishSpan: Unit = ()
}

class NullTracing extends Tracing {
  override def traceRequests: Directive1[RequestTracing] = Directives.provide(new NullRequestTracing)

  override def shutdown: Unit = ()
}
