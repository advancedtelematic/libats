package com.advancedtelematic.metrics

import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.messaging.ListenerMonitor
import io.prometheus.client.{CollectorRegistry, Counter}

import scala.concurrent.Future

class PrometheusMessagingMonitor(registry: CollectorRegistry, queue: String) extends ListenerMonitor {
  private lazy val processed =
    Counter.build().name("bus-listener.processed").labelNames(queue).create().register[Counter](registry)

  private lazy val error =
    Counter.build().name("bus-listener.error").labelNames(queue).create().register[Counter](registry)

  private lazy val restarts =
    Counter.build().name("bus-listener.restarts").labelNames(queue).create().register[Counter](registry)

  override def onProcessed: Future[Unit] = FastFuture.successful(processed.inc())

  override def onError(cause: Throwable): Future[Unit] = FastFuture.successful(error.inc())

  override def onFinished: Future[Unit] = FastFuture.successful(restarts.inc())
}
