package com.advancedtelematic.libats.messaging

import akka.Done
import akka.actor.{ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.messaging.MsgOperation.MsgOperation
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import com.codahale.metrics.MetricRegistry
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}


object MsgOperation {
  type MsgOperation[T] = T => Future[_]

  def logFailed[T](fn: MsgOperation[T])(implicit log: LoggingAdapter, ec: ExecutionContext): MsgOperation[T] = (msg: T) => {
    fn(msg).recoverWith {
      case ex =>
        log.error(ex, s"[Fatal] Could not process message $msg")
        FastFuture.failed(ex)
    }
  }

  def recoverFailed[T](fn: MsgOperation[T])(implicit log: LoggingAdapter, ec: ExecutionContext): MsgOperation[T] = (msg: T) => {
    fn(msg).recoverWith {
      case ex =>
        log.error(ex, s"[NonFatal] Could not process message $msg")
        Future.successful(Done)
    }
  }
}

object MessageListener {
  def props[T](config: Config, op: MsgOperation[T], metricRegistry: MetricRegistry)
              (implicit system: ActorSystem, ex: ExecutionContext, ml: MessageLike[T]): Props = {
    val source = MessageBus.subscribeCommittable(config, op)
    val monitor = new MetricsBusMonitor(metricRegistry, ml.streamName)

    MessageBusListenerActor.props[T](source, monitor)(ml)
  }
}
