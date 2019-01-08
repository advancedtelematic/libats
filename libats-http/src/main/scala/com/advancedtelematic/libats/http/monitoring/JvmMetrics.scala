package com.advancedtelematic.libats.http.monitoring

import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.http.HealthMetrics
import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, MemoryUsageGaugeSet}
import com.codahale.metrics.{MetricFilter, MetricRegistry, MetricSet}
import io.circe.Json
import io.circe.syntax._
import scala.collection.JavaConverters._
import scala.concurrent.Future

trait JvmMetricsSupport {
  self: MetricsSupport =>

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

class JvmMetrics(metrics: MetricRegistry) extends HealthMetrics {

  val filter: MetricFilter = (name: String, _) => name.startsWith("jvm")

  override def urlPrefix: String = "jvm"

  override def metricsJson: Future[Json] = FastFuture.successful {
    val jvm = metrics.getGauges(filter).asScala
    val data = jvm.mapValues(_.getValue.toString)
    data.toMap.asJson
  }
}
