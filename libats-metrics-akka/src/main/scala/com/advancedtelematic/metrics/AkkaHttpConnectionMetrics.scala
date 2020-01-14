package com.advancedtelematic.metrics

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, RoutingLog}
import akka.http.scaladsl.settings.{ConnectionPoolSettings, ParserSettings, RoutingSettings, ServerSettings}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.advancedtelematic.libats.http.BootApp
import com.codahale.metrics.{Gauge, MetricRegistry}

import scala.concurrent.ExecutionContextExecutor

trait AkkaHttpConnectionMetrics {
  self: BootApp with MetricsSupport =>

  private lazy val serverSettings = ServerSettings(system)

  metricRegistry.register("akka.http.max-connections", new Gauge[Long]() {
    override def getValue: Long = serverSettings.maxConnections
  })

  private lazy val connectionPoolSettings = ConnectionPoolSettings(system)

  metricRegistry.register("akka.http.host-connection-pool.max-connections", new Gauge[Long]() {
    override def getValue: Long = connectionPoolSettings.maxConnections
  })

  metricRegistry.register("akka.http.host-connection-pool.max-open-requests", new Gauge[Long]() {
    override def getValue: Long = connectionPoolSettings.maxOpenRequests
  })

  def withConnectionMetrics(routes: Route, metricRegistry: MetricRegistry)(implicit
                                                                           routingSettings: RoutingSettings,
                                                                           parserSettings: ParserSettings,
                                                                           materializer: Materializer,
                                                                           routingLog: RoutingLog,
                                                                           _executionContext: ExecutionContextExecutor = null,
                                                                           rejectionHandler: RejectionHandler = RejectionHandler.default,
                                                                           exceptionHandler: ExceptionHandler = null): Flow[HttpRequest, HttpResponse, NotUsed] =
    AkkaHttpConnectionMetricsRoutes(routes, metricRegistry)(
      routingSettings = implicitly,
      parserSettings = implicitly,
      materializer = implicitly,
      routingLog = implicitly,
      executionContext = _executionContext,
      rejectionHandler = implicitly,
      exceptionHandler = implicitly)
}

object AkkaHttpConnectionMetricsRoutes {
  def apply(routes: Route, metricRegistry: MetricRegistry)(implicit
                                                           routingSettings: RoutingSettings,
                                                           parserSettings: ParserSettings,
                                                           materializer: Materializer,
                                                           routingLog: RoutingLog,
                                                           executionContext: ExecutionContextExecutor = null,
                                                           rejectionHandler: RejectionHandler = RejectionHandler.default,
                                                           exceptionHandler: ExceptionHandler = null): Flow[HttpRequest, HttpResponse, NotUsed] = {
    val handler = Route.handlerFlow(routes)

    Flow[HttpRequest]
      .watchTermination() {
        case (mat, completionF) =>
          metricRegistry.counter("akka.http.connections").inc()
          metricRegistry.counter("akka.http.connected").inc()
          completionF.onComplete(_ => metricRegistry.counter("akka.http.connected").dec())
          mat
      }.via(handler)
  }
}
