package com.advancedtelematic.libats.slick.monitoring

import java.lang.management.ManagementFactory
import javax.management.{JMX, ObjectName}

import akka.event.LoggingAdapter
import com.advancedtelematic.libats.http.monitoring.MetricsSupport
import com.advancedtelematic.libats.http.{HealthCheck, HealthMetrics, HealthResource}
import com.codahale.metrics.MetricRegistry
import com.zaxxer.hikari.HikariPoolMXBean
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.MySQLProfile.api._
import io.circe.syntax._

import scala.util.Failure

class DbHealthMetrics()(implicit db: Database, ec: ExecutionContext) extends HealthMetrics {
  private lazy val mBeanServer = ManagementFactory.getPlatformMBeanServer
  // TODO: Use proper db name after upgrading slick (https://github.com/slick/slick/issues/1326)
  private lazy val poolName = new ObjectName("com.zaxxer.hikari:type=Pool (database)")
  private lazy val poolProxy = JMX.newMXBeanProxy(mBeanServer, poolName, classOf[HikariPoolMXBean])


  private def dbVersion(): Future[String] = {
    val query = sql"SELECT VERSION()".as[String].head
    db.run(query)
  }

  override def metricsJson: Future[Json] = {
    dbVersion().map { v =>
      Json.obj(
        "db_version" -> v.asJson,
        "idle_count" -> poolProxy.getIdleConnections.asJson,
        "active_count" -> poolProxy.getActiveConnections.asJson,
        "threads_waiting" -> poolProxy.getThreadsAwaitingConnection.asJson,
        "total_count" -> poolProxy.getTotalConnections.asJson
      )
    }
  }

  override def urlPrefix: String = "db"
}

class DbHealthCheck()(implicit db: Database, ec: ExecutionContext) extends HealthCheck {
  def apply(log: LoggingAdapter)(implicit ec: ExecutionContext): Future[Unit] = {
    val query = sql"SELECT 1 FROM dual ".as[Int]
    db
      .run(query)
      .map(_ => ())
      .andThen {
        case Failure(ex) =>
          log.error(ex, "Could not connect to db")
      }
  }
}

object DbHealthResource {
  def apply(versionRepr: Map[String, Any] = Map.empty,
            healthChecks: Seq[HealthCheck] = Seq.empty,
            healthMetrics: Seq[HealthMetrics] = Seq.empty,
            metricRegistry: MetricRegistry = MetricsSupport.metricRegistry)(implicit db: Database, ec: ExecutionContext) = {
    new HealthResource(versionRepr, new DbHealthCheck() +: healthChecks, new DbHealthMetrics() +: healthMetrics, metricRegistry)
  }
}
