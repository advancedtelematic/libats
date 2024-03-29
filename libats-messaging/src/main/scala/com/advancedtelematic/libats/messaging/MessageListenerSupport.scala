package com.advancedtelematic.libats.messaging

import akka.actor.ActorRef
import com.advancedtelematic.libats.http.BootApp
import com.advancedtelematic.libats.messaging.MsgOperation.MsgOperation
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor.Subscribe
import com.advancedtelematic.libats.messaging_datatype.MessageLike

trait MessageListenerSupport {
  self: BootApp =>

  def startListener[T](op: MsgOperation[T], busListenerMonitor: ListenerMonitor, skipProcessingErrors: Boolean = false, useCommittableSource: Boolean = true)
                      (implicit ml: MessageLike[T]): ActorRef = {
    val loggedOperation =
      if(skipProcessingErrors)
        MsgOperation.recoverFailed(op)(system.log, system.dispatcher)
      else
        MsgOperation.logFailed(op)(system.log, system.dispatcher)

    val ref = system.actorOf(MessageListener.props[T](config, loggedOperation, busListenerMonitor, useCommittableSource),  ml.streamName + "-listener")
    ref ! Subscribe
    ref
  }
}
