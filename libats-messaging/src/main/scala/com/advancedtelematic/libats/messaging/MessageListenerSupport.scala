package com.advancedtelematic.libats.messaging

import akka.actor.ActorRef
import com.advancedtelematic.libats.boot.VersionInfo
import com.advancedtelematic.libats.http.BootApp
import com.advancedtelematic.libats.messaging.MsgOperation.MsgOperation
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor.Subscribe
import com.advancedtelematic.libats.messaging_datatype.MessageLike

trait MessageListenerSupport {
  self: BootApp with VersionInfo =>

  import system.dispatcher

  def startListener[T](op: MsgOperation[T], busListenerMonitor: ListenerMonitor, skipProcessingErrors: Boolean = false)
                      (implicit ml: MessageLike[T]): ActorRef = {
    val loggedOperation =
      if(skipProcessingErrors)
        MsgOperation.recoverFailed(op)(system.log, system.dispatcher)
      else
        MsgOperation.logFailed(op)(system.log, system.dispatcher)

    val groupId = if (appConfig.hasPath("messaging.group-id"))
      appConfig.getString("messaging.group-id")
    else
      projectName

    val ref = system.actorOf(MessageListener.props[T](appConfig, loggedOperation, groupId, busListenerMonitor),  ml.streamName + "-listener")
    ref ! Subscribe
    ref
  }
}
