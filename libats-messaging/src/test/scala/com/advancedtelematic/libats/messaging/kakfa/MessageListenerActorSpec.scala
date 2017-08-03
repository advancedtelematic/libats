package com.advancedtelematic.libats.messaging

import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import com.advancedtelematic.libats.messaging.Messages.MessageLike
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor.Subscribe
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.{Future, Promise}
import scala.util.control.NoStackTrace

case class MsgListenerSpecItem(id: Int, payload: String)

object MsgListenerSpecItem {
  implicit val messageLike = MessageLike[MsgListenerSpecItem](_.id.toString)

  case object MsgListenerSpecError extends Exception("test ERROR") with NoStackTrace
}

class StorageListenerMonitor extends ListenerMonitor {
  var processed = 0
  var error = 0
  var finished = 0

  override def onProcessed: Future[Unit] = Future.successful(processed += 1)

  override def onError(cause: Throwable): Future[Unit] = Future.successful(error += 1)

  override def onFinished: Future[Unit] = Future.successful(finished += 1)
}

class MessageListenerActorSpec extends TestKit(ActorSystem("KafkaClientSpec"))
  with FunSuiteLike
  with Matchers
  with BeforeAndAfterAll
  with ScalaFutures
  with PatienceConfiguration  {

  import MsgListenerSpecItem._

  implicit val _ec = system.dispatcher
  implicit val _mat = ActorMaterializer()

  val msg = MsgListenerSpecItem(1, "payload")

  override implicit def patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  test("message listener source monitors message events") {
    val source: Source[MsgListenerSpecItem, NotUsed] = Source(List(msg))
    val monitor = new StorageListenerMonitor
    val sink = Sink.actorRef(testActor, Done).mapMaterializedValue(_ => Future.successful(Done))
    val actor = system.actorOf(Props(new MessageBusListenerActor(source, monitor, sink)))

    actor ! Subscribe

    expectMsgType[MsgListenerSpecItem]
    expectMsg(Done)

    monitor.error shouldBe 0
    monitor.finished shouldBe 1
    monitor.processed shouldBe 1
  }

  test("monitor receives errors on error") {
    val source: Source[MsgListenerSpecItem, NotUsed] = Source.failed(MsgListenerSpecError)

    val monitor = new StorageListenerMonitor

    val promise = Promise[Done]()

    val sink = Sink.ignore.mapMaterializedValue { doneF =>
      doneF.onComplete(promise.complete)
      doneF
    }

    val actor = system.actorOf(Props(new MessageBusListenerActor(source, monitor, sink)))

    actor ! Subscribe

    promise.future.failed.futureValue

    monitor.error shouldBe 1
    monitor.finished shouldBe 0
    monitor.processed shouldBe 0
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }
}
