/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package com.advancedtelematic.libats.monitoring

import java.util.concurrent.TimeUnit

import akka.event.LoggingAdapter
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.http.{HealthCheck, HealthMetrics}
import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, MemoryUsageGaugeSet}

import scala.collection.JavaConverters._
import com.codahale.metrics._
import io.circe.Json
import io.circe.syntax._

import scala.concurrent.Future

object MetricsSupport {
  lazy val metricRegistry = new MetricRegistry()

  val DbFilter = new MetricFilter {
    override def matches(name: String, metric: Metric): Boolean = name.startsWith("database")
  }
}

class JvmMetrics(metrics: MetricRegistry) extends HealthMetrics {
  val filter = new MetricFilter {
    override def matches(name: String, metric: Metric): Boolean = name.startsWith("jvm")
  }

  override def urlPrefix: String = "jvm"

  override def metricsJson: Future[Json] = FastFuture.successful {
    val jvm = metrics.getGauges(filter).asScala
    val data = jvm.mapValues(_.getValue.toString)
    data.toMap.asJson
  }
}

trait MetricsSupport {
  lazy val metricRegistry = MetricsSupport.metricRegistry

  private def registerAll(registry: MetricRegistry, prefix: String, metricSet: MetricSet): Unit = {
    metricSet.getMetrics.asScala.foreach {
      case (metricPrefix, set: MetricSet) =>
        registerAll(registry, prefix + "." + metricPrefix, set)
      case (metricPrefix, metric) =>
        registry.register(prefix + "." + metricPrefix, metric)
    }
  }

  registerAll(metricRegistry, "jvm.gc", new GarbageCollectorMetricSet())
  registerAll(metricRegistry, "jvm.memory", new MemoryUsageGaugeSet())
}
