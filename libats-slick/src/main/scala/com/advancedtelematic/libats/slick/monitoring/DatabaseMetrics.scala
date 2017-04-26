package com.advancedtelematic.libats.slick.monitoring

import java.util
import java.lang.management.ManagementFactory
import javax.management.{JMX, ObjectName}

import com.advancedtelematic.libats.monitoring.MetricsSupport
import com.advancedtelematic.libats.slick.db.DatabaseConfig
import com.codahale.metrics.{Gauge, Metric, MetricSet}
import slick.jdbc.hikaricp.HikariCPJdbcDataSource
import slick.util.AsyncExecutorMXBean

trait DatabaseMetrics { self: MetricsSupport with DatabaseConfig =>

  val hikariDatasource = db.source.asInstanceOf[HikariCPJdbcDataSource]
  hikariDatasource.ds.setMetricRegistry(metricRegistry)

  private def gauge[A](f: () => A): Gauge[A] = new Gauge[A] {
    override def getValue: A = f.apply()
  }

  val executorMXBean = {
    val mbeanServer = ManagementFactory.getPlatformMBeanServer
    val poolName = hikariDatasource.hconf.getPoolName
    db.executor.executionContext // create execution context and register MXBean
    val mbean = JMX.newMXBeanProxy(
      mbeanServer,
      new ObjectName( s"slick:type=AsyncExecutor,name=$poolName"),
      classOf[AsyncExecutorMXBean])
    metricRegistry.register(s"slick.$poolName", new MetricSet {
      override def getMetrics: util.Map[String, Metric] = {
        import scala.collection.JavaConverters._
        Map[String, Metric](
          "threads.active" -> gauge(() => mbean.getActiveThreads),
          "threads.max" -> gauge(() => mbean.getMaxThreads),
          "queue.size" -> gauge(() => mbean.getQueueSize),
          "queue.maxsize" -> gauge(() => mbean.getMaxQueueSize)
        ).asJava
      }
    })
  }
}
