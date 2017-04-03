/*
 * Copyright 2016 ATS Advanced Telematic Systems GmbH
 */
package com.advancedtelematic.metrics

import java.util

import com.codahale.metrics.Gauge
import org.apache.kafka.common.metrics.{KafkaMetric, MetricsReporter}

/**
  * Reports kafka metrics to dropwizard metrics.
  */
class KafkaMetrics extends MetricsReporter {
  import com.advancedtelematic.metrics.DropwizardMetrics.registry

  import scala.collection.JavaConverters._

  private[this] def shortName(m: KafkaMetric): String = {
    val mn = m.metricName()
    s"kafka/${mn.group()}/${mn.name()}"
  }

  private[this] def metricName(m: KafkaMetric): String = {
    val mn = m.metricName()
    s"kafka/${mn.group()}/${mn.name()}/${mn.tags().asScala.map(x => s"${x._1}/${x._2}").mkString("/")}"
  }

  override def init(metrics: util.List[KafkaMetric]): Unit = {
    metrics.asScala
      .foreach(
        x =>
          registry.register(metricName(x), NamedMetric(new Gauge[Double] {
            override def getValue: Double = x.value()
          }, shortName(x), x.metricName().tags().asScala.toMap))
      )
  }

  override def metricRemoval(metric: KafkaMetric): Unit = {
    registry.remove(metricName(metric))
  }

  override def close(): Unit = {}

  override def metricChange(metric: KafkaMetric): Unit = {
    registry.register(
      metricName(metric),
      NamedMetric(new Gauge[Double] {
        override def getValue: Double = metric.value()
      }, shortName(metric), metric.metricName().tags().asScala.toMap)
    )
  }

  override def configure(configs: util.Map[String, _]): Unit = {}
}
