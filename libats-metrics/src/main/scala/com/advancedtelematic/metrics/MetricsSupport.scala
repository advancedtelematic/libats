/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package com.advancedtelematic.metrics

import com.codahale.metrics.MetricRegistry

object MetricsSupport {
  lazy val metricRegistry = new MetricRegistry()
}

trait MetricsSupport extends JvmMetricsSupport with LoggerMetricsSupport {
  val metricRegistry: MetricRegistry
}
