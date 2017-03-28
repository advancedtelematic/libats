/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.http

import java.lang.management.ManagementFactory
import javax.management.{JMX, ObjectName}

import akka.http.scaladsl.server.Directives
import com.advancedtelematic.libats.monitoring.MetricsSupport
import com.zaxxer.hikari.pool.HikariPoolMBean
import slick.driver.MySQLDriver.api._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import com.advancedtelematic.libats.codecs.AkkaCirce._
import de.heikoseeberger.akkahttpcirce.CirceSupport._
import io.circe.syntax._

import scala.util.Failure

class HealthResource(db: Database, versionRepr: Map[String, Any] = Map.empty)
                    (implicit val ec: ExecutionContext) {
  import Directives._

  private lazy val mBeanServer = ManagementFactory.getPlatformMBeanServer
  // TODO: Use proper db name after upgrading slick (https://github.com/slick/slick/issues/1326)
  private lazy val poolName = new ObjectName("com.zaxxer.hikari:type=Pool (database)")
  private lazy val poolProxy = JMX.newMXBeanProxy(mBeanServer, poolName, classOf[HikariPoolMBean])

  val metricRegistry = MetricsSupport.metricRegistry

  private def dbVersion(): Future[String] = {
    val query = sql"SELECT VERSION()".as[String].head
    db.run(query)
  }

  val route =
    (get & pathPrefix("health") & extractLog) { logger =>
      pathEnd {
        val query = sql"SELECT 1 FROM dual ".as[Int]
        val f = db
          .run(query)
          .map(_ => Map("status" -> "OK"))
          .andThen {
            case Failure(ex) =>
              logger.error(ex, "Could not connect to db")
          }
        complete(f)
      } ~
      path("version") {
        val f = dbVersion().map { v =>
          (versionRepr.mapValues(_.toString) + ("dbVersion" -> v)).asJson
        }

        complete(f)
      } ~
      path("db") {
        val data = Map(
          "idle_count" -> poolProxy.getIdleConnections,
          "active_count" -> poolProxy.getActiveConnections,
          "threads_waiting" -> poolProxy.getThreadsAwaitingConnection,
          "total_count" -> poolProxy.getTotalConnections
        )

        complete(data.asJson)
      } ~
      path("jvm") {
        val jvm = metricRegistry.getGauges(MetricsSupport.JvmFilter).asScala
        val data = jvm.mapValues(_.getValue.toString)

        complete(data.toMap.asJson)
      }
    }
}
