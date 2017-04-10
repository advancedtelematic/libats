package com.advancedtelematic.libats.slick.monitoring

import com.advancedtelematic.libats.monitoring.MetricsSupport
import com.advancedtelematic.libats.slick.db.DatabaseConfig
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

trait DatabaseMetrics {
  self: MetricsSupport with DatabaseConfig =>

  db.source.asInstanceOf[HikariCPJdbcDataSource].ds.setMetricRegistry(metricRegistry)
}
