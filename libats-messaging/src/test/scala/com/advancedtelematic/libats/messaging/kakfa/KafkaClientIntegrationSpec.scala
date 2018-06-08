/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package com.advancedtelematic.libats.messaging.kakfa

import java.time.Instant

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import com.advancedtelematic.libats.messaging.kafka.KafkaClient
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.Future


case class KafkaSpecMessage(id: Int, payload: String)

object KafkaSpecMessage {
  implicit val messageLike = MessageLike[KafkaSpecMessage](_.id.toString)
}

class KafkaClientIntegrationSpec extends TestKit(ActorSystem("KafkaClientSpec"))
  with FunSuiteLike
  with Matchers
  with BeforeAndAfterAll
  with ScalaFutures
  with PatienceConfiguration  {

  implicit val _ec = system.dispatcher
  implicit val _mat = ActorMaterializer()

  override implicit def patienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  val publisher = KafkaClient.publisher(system, system.settings.config)

  test("can send an event to bus") {
    val testMsg = KafkaSpecMessage(1, Instant.now.toString)
    val f = publisher.publish(testMsg).map(_ => 0)
    f.futureValue shouldBe 0
  }

  test("can send-receive events from bus") {
    val testMsg = KafkaSpecMessage(2, Instant.now.toString)

    val source = KafkaClient.source[KafkaSpecMessage](system, system.settings.config)
    val msgFuture = source.groupedWithin(10, 5.seconds).runWith(Sink.head)

    for {
      _ <- akka.pattern.after(3.seconds, system.scheduler)(Future.successful(()))
      _ <- publisher.publish(testMsg)
    } yield ()

    msgFuture.futureValue should contain(testMsg)
  }

  test("can send-receive and commit events from bus") {
    val testMsg = KafkaSpecMessage(3, Instant.now.toString)

    val cfg = ConfigFactory.parseString(
      """
        |messaging.listener.parallelism=2
        |messaging.listener.batch.interval=5s
        |messaging.listener.batch.max=10
      """.stripMargin).withFallback(system.settings.config)
    val source = KafkaClient.committableSource[KafkaSpecMessage](cfg, (_: KafkaSpecMessage) => FastFuture.successful(Done))

    val msgFuture = source
//      .groupedWithin(10, 5.seconds)
//      .map(g => (g.foldLeft(CommittableOffsetBatch.empty) { (batch, elem) => batch.updated(elem.committableOffset) }, g))
//      .mapAsync(1) { case (offsets, elements) => offsets.commitScaladsl().map(_ => elements.map(_.record.value())) }
      .runWith(Sink.head)

    for {
      _ <- akka.pattern.after(3.seconds, system.scheduler)(Future.successful(()))
      _ <- publisher.publish(testMsg)
    } yield ()

    msgFuture.futureValue should equal(testMsg)
  }
}
