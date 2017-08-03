package com.advancedtelematic.libats.messaging

import akka.actor.ActorRef
import com.advancedtelematic.libats.http.BootApp
import com.advancedtelematic.libats.messaging.MessageListener.MsgOperation
import com.advancedtelematic.libats.messaging.Messages.MessageLike
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor.Subscribe
import com.advancedtelematic.libats.monitoring.MetricsSupport

trait MessageListenerSupport {
  self: BootApp with MetricsSupport =>

  def startListener[T](op: MsgOperation[T])
                      (implicit ml: MessageLike[T]): ActorRef = {
    val ref = system.actorOf(MessageListener.props[T](config, op, metricRegistry),  ml.streamName + "-listener")
    ref ! Subscribe
    ref
  }
}
