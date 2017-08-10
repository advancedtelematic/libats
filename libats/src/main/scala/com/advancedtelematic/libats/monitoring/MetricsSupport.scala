/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package com.advancedtelematic.libats.monitoring

import com.codahale.metrics._

object MetricsSupport {
  lazy val metricRegistry = new MetricRegistry()
}

trait MetricsSupport extends JvmMetricsSupport with LoggerMetricsSupport {
  lazy val metricRegistry = MetricsSupport.metricRegistry
}
