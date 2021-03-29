package com.advancedtelematic.libats.messaging

import akka.Done
import akka.actor.{ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.messaging.MsgOperation.MsgOperation
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal


object MsgOperation {
  type MsgOperation[T] = T => Future[_]

  def logFailed[T](fn: MsgOperation[T])(implicit log: LoggingAdapter, ec: ExecutionContext): MsgOperation[T] = (msg: T) => {
    try {
      fn(msg).recoverWith {
        case ex =>
          log.error(ex, s"[Fatal] MsgOperation returned a failed future:  $msg")
          FastFuture.failed(ex)
      }
    } catch {
      case NonFatal(ex) =>
        log.error(ex, s"[NonFatal] MsgOperation could not return a valid future: $msg")
        FastFuture.failed(ex)
    }
  }

  def recoverFailed[T](fn: MsgOperation[T])(implicit log: LoggingAdapter, ec: ExecutionContext): MsgOperation[T] = (msg: T) => {
    try {
      fn(msg).recoverWith {
        case ex =>
          log.error(ex, s"[NonFatal] MsgOperation returned a failed future:  $msg")
          Future.successful(Done)
      }
    } catch {
      case NonFatal(ex) =>
        log.error(ex, s"[NonFatal] MsgOperation could not return a valid future: $msg")
        Future.successful(Done)
    }
  }
}

object MessageListener {
  def props[T](config: Config, op: MsgOperation[T], groupId: String, busMonitor: ListenerMonitor = LoggingListenerMonitor)
              (implicit system: ActorSystem, ex: ExecutionContext, ml: MessageLike[T]): Props = {
    val source = MessageBus.subscribeCommittable(config, groupId, op)
    MessageBusListenerActor.props[T](source, busMonitor)(ml)
  }
}
