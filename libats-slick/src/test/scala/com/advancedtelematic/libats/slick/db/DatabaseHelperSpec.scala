package com.advancedtelematic.libats.slick.db

import akka.actor.{ActorSystem, Scheduler}
import com.advancedtelematic.libats.slick.db.DatabaseHelper.DatabaseWithRetry
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.when
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import slick.dbio.DBIOAction
import slick.jdbc.JdbcBackend.Database

import java.sql.SQLTransientConnectionException
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class DatabaseHelperSpec extends FunSuite with Matchers with ScalaFutures {
  implicit lazy val system = ActorSystem(this.getClass.getSimpleName)
  implicit val scheduler: Scheduler = system.scheduler

  val dbMock = mock[Database]
  val action = DBIOAction.from(Future.successful(anyInt()))
  val action2 = DBIOAction.from(Future.failed(new RuntimeException()))
  val failedCall = Future.failed(new SQLTransientConnectionException())
  val successfulCall = Future.successful(1)
  val dbWithRetry = new DatabaseWithRetry(dbMock)

  test("should be successful when database is available in a subsequent call") {
    when(dbMock.run(action)).thenReturn(failedCall, successfulCall)
    val result = dbWithRetry.runWithRetry(action, List(1.millis))
    whenReady(result) { _ shouldBe 1 }
  }

  test("should fail when database is not available in all calls") {
    when(dbMock.run(action)).thenReturn(failedCall)
    val result = dbWithRetry.runWithRetry(action, List(1.millis))
    whenReady(result.failed) { _ shouldBe a [SQLTransientConnectionException] }
  }
}
