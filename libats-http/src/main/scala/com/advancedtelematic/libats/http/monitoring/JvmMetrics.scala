package com.advancedtelematic.libats.http.monitoring

import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.http.HealthMetrics
import com.advancedtelematic.metrics.OsMetricSet
import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet}
import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry, MetricSet}
import io.circe.Json
import io.circe.syntax._

import scala.collection.JavaConversions._
import scala.concurrent.Future


trait JvmMetricsSupport {
  self: MetricsSupport =>

  private def registerAll(registry: MetricRegistry, prefix: String, metricSet: MetricSet): Unit = {
    metricSet.getMetrics.foreach {
      case (metricPrefix, set: MetricSet) =>
        registerAll(registry, prefix + "." + metricPrefix, set)
      case (metricPrefix, metric) =>
        registry.register(prefix + "." + metricPrefix, metric)
    }
  }

  registerAll(metricRegistry, "jvm.gc", new GarbageCollectorMetricSet())
  registerAll(metricRegistry, "jvm.memory", new MemoryUsageGaugeSet())
  registerAll(metricRegistry, "jvm.os", OsMetricSet)
  registerAll(metricRegistry, "jvm.thread", new ThreadStatesGaugeSet())
}

class JvmMetrics(metrics: MetricRegistry) extends HealthMetrics {
  val filter = new MetricFilter {
    override def matches(name: String, metric: Metric): Boolean = name.startsWith("jvm")
  }

  override def urlPrefix: String = "jvm"

  override def metricsJson: Future[Json] = FastFuture.successful {
    val jvm = metrics.getGauges(filter)
    val data = jvm.mapValues(_.getValue.toString)
    data.toMap.asJson
  }
}
