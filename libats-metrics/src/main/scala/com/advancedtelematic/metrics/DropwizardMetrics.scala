/*
 * Copyright 2016 ATS Advanced Telematic Systems GmbH
 */
package com.advancedtelematic.metrics

import java.lang.management.ManagementFactory
import java.util
import java.util.Collections

import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet}
import com.codahale.metrics.{Gauge, Metric, MetricRegistry, MetricSet}

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

object DropwizardMetrics {
  val registry = new MetricRegistry
  registry.register("jvm.gc", new GarbageCollectorMetricSet)
  registry.register("jvm.mem", new MemoryUsageGaugeSet())
  registry.register("jvm.thread", new ThreadStatesGaugeSet())
  registry.register("jvm.os", OsMetricSet)
}
