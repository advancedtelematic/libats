package com.advancedtelematic.metrics.prometheus

import java.io.StringWriter

import akka.http.scaladsl.server.Route
import com.advancedtelematic.libats.http.BootApp
import com.advancedtelematic.libats.http.monitoring.MetricsSupport
import akka.http.scaladsl.server.Directives._
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.dropwizard.DropwizardExports
import io.prometheus.client.exporter.common.TextFormat

import scala.collection.JavaConverters._

object PrometheusMetricsRoutes {
  def apply(registry: CollectorRegistry): Route = {
    (get & path("metrics") & parameter('name.*)) { names =>
      val stringWriter = new StringWriter()
      TextFormat.write004(stringWriter, registry.filteredMetricFamilySamples(names.toSet.asJava))
      complete(stringWriter.toString)
    }
  }

}

trait PrometheusMetricsSupport {
  self: BootApp with MetricsSupport =>

  CollectorRegistry.defaultRegistry.register(new DropwizardExports(metricRegistry))

  val prometheusMetricsRoutes: Route = PrometheusMetricsRoutes(CollectorRegistry.defaultRegistry)
}
