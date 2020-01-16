package com.advancedtelematic.metrics

import java.lang.management.ManagementFactory
import java.util
import java.util.Collections

import akka.http.scaladsl.util.FastFuture
import com.codahale.metrics.{Gauge, Metric, MetricFilter, MetricRegistry, MetricSet}
import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet}
import io.circe.Json
import io.circe.syntax._

import scala.collection.JavaConverters._
import scala.concurrent.Future


object OsMetricSet extends MetricSet {
  val os = ManagementFactory.getOperatingSystemMXBean
  override def getMetrics: util.Map[String, Metric] = {
    os match {
      case unix: com.sun.management.UnixOperatingSystemMXBean =>
        import scala.collection.JavaConverters._
        Map[String, Metric](
          "cpu-load.system" -> new Gauge[Double] {
            override def getValue: Double = unix.getSystemCpuLoad
          },
          "cpu-load.process" -> new Gauge[Double] {
            override def getValue: Double = unix.getProcessCpuLoad
          }
        ).asJava
      case _ => Collections.emptyMap[String, Metric]()
    }
  }
}

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
  registerAll(metricRegistry, "jvm.os", OsMetricSet)
  registerAll(metricRegistry, "jvm.thread", new ThreadStatesGaugeSet())
}

class JvmMetrics(metrics: MetricRegistry) extends MetricsRepresentation {
  val filter = new MetricFilter {
    override def matches(name: String, metric: Metric): Boolean = name.startsWith("jvm")
  }

  override def urlPrefix: String = "jvm"

  override def metricsJson: Future[Json] = FastFuture.successful {
    val jvm = metrics.getGauges(filter)
    val data = jvm.asScala.mapValues(_.getValue.toString)
    data.toMap.asJson
  }
}
