package com.advancedtelematic.metrics

import akka.http.scaladsl.util.FastFuture
import ch.qos.logback.classic.LoggerContext
import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import com.codahale.metrics.logback.InstrumentedAppender
import io.circe.Json
import io.circe.syntax._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
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

class LoggerMetrics(metricRegistry: MetricRegistry) extends MetricsRepresentation {

  lazy val filter: MetricFilter = (name: String, _: Metric) => name.startsWith("log")

  override def metricsJson: Future[Json] = FastFuture.successful {
    val meters = metricRegistry.getMeters(filter)

    meters.asScala.mapValues { v =>
      Map("count" -> v.getCount.asJson, "rate.1m" -> v.getOneMinuteRate.asJson)
    }.toMap.asJson
  }

  override def urlPrefix: String = "log"
}
