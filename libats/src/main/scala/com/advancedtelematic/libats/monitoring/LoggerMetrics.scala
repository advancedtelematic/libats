package com.advancedtelematic.libats.monitoring

import akka.http.scaladsl.util.FastFuture
import ch.qos.logback.classic.LoggerContext
import com.advancedtelematic.libats.http.HealthMetrics
import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import com.codahale.metrics.logback.InstrumentedAppender
import io.circe.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Future

trait LoggerMetricsSupport {
  self: MetricsSupport =>

  val factory = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
  val root = factory.getLogger(Logger.ROOT_LOGGER_NAME)

  val metrics = new InstrumentedAppender(metricRegistry)
  metrics.setContext(root.getLoggerContext)

  metrics.setName("log")

  metrics.start()

  root.addAppender(metrics)
}

class LoggerMetrics(metricRegistry: MetricRegistry) extends HealthMetrics {
  import scala.collection.JavaConversions._
  import io.circe.syntax._

  lazy val filter: MetricFilter = new MetricFilter {
    override def matches(name: String, metric: Metric): Boolean = name.startsWith("log")
  }

  override def metricsJson: Future[Json] = FastFuture.successful {
    val meters = metricRegistry.getMeters(filter)

    meters.mapValues { v =>
      Map("count" -> v.getCount.asJson, "rate.1m" -> v.getOneMinuteRate.asJson)
    }.toMap.asJson
  }

  override def urlPrefix: String = "log"
}
