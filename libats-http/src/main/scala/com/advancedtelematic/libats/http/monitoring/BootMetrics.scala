package com.advancedtelematic.libats.http.monitoring

import com.advancedtelematic.libats.http.BootApp

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry

trait BootMetrics {
  self: BootApp with MetricsSupport =>

  val version: String

  val builtAtMillis: Long

  metricRegistry.register(MetricRegistry.name(projectName, "version_hashcode"), new Gauge[Integer]() {
    override def getValue: Integer = version.hashCode
  })

  metricRegistry.register(MetricRegistry.name(projectName, "built_at"), new Gauge[Long]() {
    override def getValue: Long = builtAtMillis
  })
}
