package com.advancedtelematic.libats.slick.monitoring

import java.lang.management.ManagementFactory
import javax.management.{JMX, ObjectName}

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import akka.event.LoggingAdapter
import com.advancedtelematic.libats.http.{HealthCheck, HealthResource}
import com.zaxxer.hikari.HikariPoolMXBean
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.MySQLProfile.api._
import io.circe.syntax._

import scala.util.Failure

object DbHealthResource {
  def apply(versionRepr: Map[String, Any])(implicit db: Database, ec: ExecutionContext): DbHealthResource =
    new DbHealthResource(Seq(DbHealthResource.HealthCheck), versionRepr)

  def HealthCheck(implicit db: Database) = new HealthCheck {
    override def apply(logger: LoggingAdapter)(implicit ec: ExecutionContext): Future[Unit] = {
      val query = sql"SELECT 1 FROM dual ".as[Int]
      db
        .run(query)
        .map(_ => ())
        .andThen {
          case Failure(ex) =>
            logger.error(ex, "Could not connect to db")
        }
    }
  }
}

class DbHealthResource(healthChecks: Seq[HealthCheck], versionRepr: Map[String, Any] = Map.empty)
                      (implicit val db: Database, override val ec: ExecutionContext)
  extends HealthResource(healthChecks, versionRepr) {

  import akka.http.scaladsl.server.Directives._

  private lazy val mBeanServer = ManagementFactory.getPlatformMBeanServer
  // TODO: Use proper db name after upgrading slick (https://github.com/slick/slick/issues/1326)
  private lazy val poolName = new ObjectName("com.zaxxer.hikari:type=Pool (database)")
  private lazy val poolProxy = JMX.newMXBeanProxy(mBeanServer, poolName, classOf[HikariPoolMXBean])

  private def dbVersion(): Future[String] = {
    val query = sql"SELECT VERSION()".as[String].head
    db.run(query)
  }

  val dbRoutes =
    path("health" / "db") {
      val f = dbVersion().map { v =>
        Json.obj(
          "db_version" -> v.asJson,
          "idle_count" -> poolProxy.getIdleConnections.asJson,
          "active_count" -> poolProxy.getActiveConnections.asJson,
          "threads_waiting" -> poolProxy.getThreadsAwaitingConnection.asJson,
          "total_count" -> poolProxy.getTotalConnections.asJson
        )
      }

      complete(f)
    }

  override def route = super.route ~ dbRoutes
}
