package com.advancedtelematic.libats.slick.db

import akka.actor.Scheduler
import akka.pattern.after
import slick.jdbc.MySQLProfile.api._

import java.sql.SQLTransientConnectionException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object DatabaseHelper {
  val DefaultBackoffs: List[FiniteDuration] = List(100.millis, 200.millis, 400.millis, 800.millis, 1600.millis, 3200.millis)

  implicit class DatabaseWithRetry(db: Database) {
    def runWithRetry[T](a: DBIOAction[T, NoStream, Nothing], backoffs: List[FiniteDuration] = DefaultBackoffs)(implicit scheduler: Scheduler): Future[T] =
      db.run(a).recoverWith {
        case _: SQLTransientConnectionException if backoffs.nonEmpty =>
          after(backoffs.head, scheduler)(runWithRetry(a, backoffs.tail))
      }
  }
}