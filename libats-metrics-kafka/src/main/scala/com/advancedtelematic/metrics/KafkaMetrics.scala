/*
 * Copyright (C) 2016 HERE Global B.V.
 *
 * Licensed under the Mozilla Public License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.mozilla.org/en-US/MPL/2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: MPL-2.0
 * License-Filename: LICENSE
 */

package com.advancedtelematic.metrics

import java.util

import com.codahale.metrics.Gauge
import org.apache.kafka.common.metrics.{KafkaMetric, MetricsReporter}

/**
  * Reports kafka metrics to dropwizard metrics.
  */
class KafkaMetrics extends MetricsReporter {
  import com.advancedtelematic.metrics.MetricsSupport.metricRegistry

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
          metricRegistry.register(metricName(x), NamedMetric(new Gauge[Double] {
            override def getValue: Double = x.value()
          }, shortName(x), x.metricName().tags().asScala.toMap))
      )
  }

  override def metricRemoval(metric: KafkaMetric): Unit = {
    metricRegistry.remove(metricName(metric))
  }

  override def close(): Unit = {}

  override def metricChange(metric: KafkaMetric): Unit = {
    metricRegistry.register(
      metricName(metric),
      NamedMetric(new Gauge[Double] {
        override def getValue: Double = metric.value()
      }, shortName(metric), metric.metricName().tags().asScala.toMap)
    )
  }

  override def configure(configs: util.Map[String, _]): Unit = {}
}
