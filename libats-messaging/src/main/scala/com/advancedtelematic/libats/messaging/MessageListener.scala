package com.advancedtelematic.libats.messaging

import akka.actor.{ActorSystem, Props}
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import com.codahale.metrics.MetricRegistry
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}


object MessageListener {
  type MsgOperation[T] = T => Future[_]

  def props[T](config: Config, op: MsgOperation[T], metricRegistry: MetricRegistry)
              (implicit system: ActorSystem, ex: ExecutionContext, ml: MessageLike[T]): Props = {
    val source = MessageBus.subscribeCommittable(config, op)
    val monitor = new MetricsBusMonitor(metricRegistry, ml.streamName)

    MessageBusListenerActor.props[T](source, monitor)(ml)
  }
}
