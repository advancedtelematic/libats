package com.advancedtelematic.libats.messaging

import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.metrics.MetricsRepresentation
import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import io.circe.Json
import org.slf4j.LoggerFactory
import io.circe.syntax._

import scala.concurrent.Future

trait ListenerMonitor {
  def onProcessed: Future[Unit]

  def onError(cause: Throwable): Future[Unit]

  def onFinished: Future[Unit]
}

object LoggingListenerMonitor extends ListenerMonitor {
  private val _log = LoggerFactory.getLogger(this.getClass)

  override def onProcessed: Future[Unit] = Future.successful(_log.info("Processed it"))

  override def onError(cause: Throwable): Future[Unit] = Future.successful(_log.warn("Error it", cause))

  override def onFinished: Future[Unit] = Future.successful(_log.warn("Listener terminated"))
}

object NoOpListenerMonitor extends ListenerMonitor {
  override def onProcessed: Future[Unit] = FastFuture.successful(())

  override def onError(cause: Throwable): Future[Unit] = FastFuture.successful(())

  override def onFinished: Future[Unit] = FastFuture.successful(())
}

class MetricsBusMonitor(metrics: MetricRegistry, queue: String) extends ListenerMonitor {
  import com.codahale.metrics.MetricRegistry.name

  private val processed = metrics.counter(name("bus-listener", queue, "processed"))
  private val error = metrics.counter(name("bus-listener", queue, "error"))
  private val restarts = metrics.counter(name("bus-listener", queue, "restarts"))

  override def onProcessed: Future[Unit] = FastFuture.successful(processed.inc(1))

  override def onError(cause: Throwable): Future[Unit] = FastFuture.successful(error.inc(1))

  override def onFinished: Future[Unit] = FastFuture.successful(restarts.inc(1))
}

class BusListenerMetrics(metrics: MetricRegistry) extends MetricsRepresentation {
  import scala.collection.JavaConverters._

  val filter = new MetricFilter {
    override def matches(name: String, metric: Metric): Boolean = name.startsWith("bus-listener")
  }

  override def metricsJson: Future[Json] = FastFuture.successful {
    val counters = metrics.getCounters(filter).asScala
    val data = counters.mapValues(_.getCount)
    data.toMap.asJson
  }

  override def urlPrefix: String = "bus-listener"
}
