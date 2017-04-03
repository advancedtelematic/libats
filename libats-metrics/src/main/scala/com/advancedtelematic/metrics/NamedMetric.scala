package com.advancedtelematic.metrics

import com.codahale.metrics.Metric

import scala.reflect.ClassTag

final case class NamedMetric[A <: Metric: ClassTag](metric: A, name: String, tags: Map[String, String]) extends Metric
