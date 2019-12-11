package com.advancedtelematic.metrics

import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.messaging.ListenerMonitor
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import io.prometheus.client.{CollectorRegistry, Counter}

import scala.concurrent.Future

object PrometheusMessagingMonitor {
  protected lazy val processed =
    Counter.build().name("bus_listener_processed")
      .help("bus listener processed")
      .labelNames("stream_name")
      .create().register[Counter]()

  protected lazy val error =
    Counter.build().name("bus_listener_error")
      .help("bus listener error")
      .labelNames("stream_name")
      .create().register[Counter]()

  protected lazy val restarts =
    Counter.build()
      .name("bus_listener_restarts")
      .help("bus listener restarts")
      .labelNames("stream_name")
      .create().register[Counter]()


  def apply[T : MessageLike]() =
    new PrometheusMessagingMonitor(implicitly[MessageLike[T]].streamName)
}

class PrometheusMessagingMonitor(streamName: String) extends ListenerMonitor {
  import PrometheusMessagingMonitor._

  override def onProcessed: Future[Unit] = FastFuture.successful(processed.labels(streamName).inc())

  override def onError(cause: Throwable): Future[Unit] = FastFuture.successful(error.labels(streamName).inc())

  override def onFinished: Future[Unit] = FastFuture.successful(restarts.labels(streamName).inc())
}
