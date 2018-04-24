package com.advancedtelematic.libats.messaging

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.util.FastFuture
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.scaladsl.Source
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor
import com.advancedtelematic.libats.messaging.kafka.Commiter
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import com.codahale.metrics.MetricRegistry
import com.typesafe.config.Config

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.Future


object MessageListener {
  type MsgOperation[T] = T => Future[_]

  trait CommittableMsg[T] {
    implicit val messageLike: MessageLike[T]
    val msg: T
  }

  class KafkaMsg[T](message: CommittableMessage[_, T])(implicit val messageLike: MessageLike[T]) extends CommittableMsg[T] {
    override val msg: T = message.record.value()

    val offset = message.committableOffset
  }

  def buildSource[T](fromSource: Source[CommittableMsg[T], NotUsed], commiter: Commiter, op: MsgOperation[T],
                     batchInterval: FiniteDuration, batchMax: Int,
                     parallelism: Int = 3)
                    (implicit system: ActorSystem, ml: MessageLike[T]): Source[T, NotUsed] = {

    implicit val ec = system.dispatcher

    val plainSource = fromSource.mapAsync(parallelism) { committableMsg =>
      op(committableMsg.msg).map(_ => committableMsg)
    }

    if(batchInterval > Duration.Zero && batchMax > 0)
      plainSource.groupedWithin(batchMax, batchInterval).mapAsync(1) {
        case xs@Nil => FastFuture.successful(xs)
        case xs =>
          system.log.info(s"message listener: Committing batch with size ${xs.size} after $batchInterval")
          commiter.commit(xs).map(_ => xs)
      }.mapConcat(_.map(_.msg))
    else
      plainSource.mapAsync(1) { commitableMsg =>
        commiter.commit(commitableMsg).map(_ => commitableMsg.msg)
      }
  }

  def props[T](config: Config, op: MsgOperation[T], metricRegistry: MetricRegistry)
              (implicit system: ActorSystem, ml: MessageLike[T]): Props = {
    val parallelism = config.getInt("messaging.listener.parallelism")
    val batch = config.getDuration("messaging.listener.batch.interval", TimeUnit.MILLISECONDS)
    val batchMax = config.getInt("messaging.listener.batch.max")

    val (committableSource, committer) = MessageBus.subscribeCommittable(config)
    val source = buildSource(committableSource, committer, op, FiniteDuration(batch, TimeUnit.MILLISECONDS), batchMax, parallelism)
    val monitor = new MetricsBusMonitor(metricRegistry, ml.streamName)

    MessageBusListenerActor.props[T](source, monitor)(ml)
  }
}
