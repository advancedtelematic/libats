package com.advancedtelematic.metrics

import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.messaging.ListenerMonitor
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import io.prometheus.client.{CollectorRegistry, Counter}

import scala.concurrent.Future

object PrometheusMessagingMonitor {
  def apply[T : MessageLike](registry: CollectorRegistry = CollectorRegistry.defaultRegistry) =
    new PrometheusMessagingMonitor(registry, implicitly[MessageLike[T]].streamName)
}

class PrometheusMessagingMonitor(registry: CollectorRegistry, streamName: String) extends ListenerMonitor {
  private lazy val processed =
    Counter.build().name("bus_listener_processed")
      .help("bus listener processed")
      .labelNames("stream_name")
      .create().register[Counter](registry)

  private lazy val error =
    Counter.build().name("bus_listener_error")
      .help("bus listener error")
      .labelNames("stream_name")
      .create().register[Counter](registry)

  private lazy val restarts =
    Counter.build()
      .name("bus_listener_restarts")
      .help("bus listener restarts")
      .labelNames("stream_name")
      .create().register[Counter](registry)

  override def onProcessed: Future[Unit] = FastFuture.successful(processed.labels(streamName).inc())

  override def onError(cause: Throwable): Future[Unit] = FastFuture.successful(error.labels(streamName).inc())

  override def onFinished: Future[Unit] = FastFuture.successful(restarts.labels(streamName).inc())
}
