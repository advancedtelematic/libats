package com.advancedtelematic.libats.messaging

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.scaladsl.Source
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import com.codahale.metrics.MetricRegistry
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}


object MessageListener {

  type MsgOperation[T] = T => Future[_]

  trait CommittableMsg[T] {
    implicit val messageLike: MessageLike[T]
    def msg(): T

    def commit()(implicit ec: ExecutionContext): Future[T]
  }

  class KafkaMsg[T](message: CommittableMessage[_, T])(implicit val messageLike: MessageLike[T]) extends CommittableMsg[T] {
    override def msg(): T = message.record.value()

    override def commit()(implicit ec: ExecutionContext): Future[T] =
      message.committableOffset.commitScaladsl().map(_ => msg())
  }

  def buildSource[T](fromSource: Source[CommittableMsg[T], NotUsed], op: MsgOperation[T])
                    (implicit system: ActorSystem, ml: MessageLike[T]): Source[T, NotUsed] = {
    implicit val ec = system.dispatcher

    fromSource.mapAsync(3) { committableMsg =>
      val msg = committableMsg.msg()
      op(msg).map(_ => committableMsg)
    }.mapAsync(1) { committableMsg =>
      committableMsg.commit()
    }
  }

  def props[T](config: Config, op: MsgOperation[T], metricRegistry: MetricRegistry)
              (implicit system: ActorSystem, ml: MessageLike[T]): Props = {
    val source = buildSource(MessageBus.subscribeCommittable(config), op)

    val monitor = new MetricsBusMonitor(metricRegistry, ml.streamName)

    MessageBusListenerActor.props[T](source, monitor)(ml)
  }
}
