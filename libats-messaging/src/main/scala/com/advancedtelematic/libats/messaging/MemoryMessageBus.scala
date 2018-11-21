package com.advancedtelematic.libats.messaging
import java.util.concurrent.ConcurrentSkipListSet

import akka.actor.ActorRef
import com.advancedtelematic.libats.messaging_datatype.MessageLike

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


class MemoryMessageBus {
  private val _receivers = new ConcurrentSkipListSet[ActorRef]()

  def subscribe[T](ref: ActorRef)(implicit m: MessageLike[T]): Unit =
    _receivers.add(ref)

  def publisher(): MessageBusPublisher = {
    new MessageBusPublisher {
      override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] = {
        Future.fromTry {
          Try {
            _receivers.forEach((receiver: ActorRef) => receiver ! msg)
          }
        }
      }
    }
  }
}
