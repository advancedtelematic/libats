package com.advancedtelematic.libats.messaging

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.advancedtelematic.libats.messaging.Messages.MessageLike

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MemoryMessageBus {
  private val _buffer = mutable.Queue.empty[Any]
  private var _receiver = Option.empty[ActorRef]

  def subscribe[T]()(implicit m: MessageLike[T]): Source[T, NotUsed] = {
    Source.actorRef(MessageBus.DEFAULT_CLIENT_BUFFER_SIZE, OverflowStrategy.dropTail).mapMaterializedValue { ref =>
      if(_receiver.isDefined)
        throw new RuntimeException("MemoryMessageBus does not support more than one subscriber")

      _receiver = Some(ref)
      _buffer.dequeueAll(_ => true).foreach(ref ! _)
      NotUsed
    }
  }

  def publisher(): MessageBusPublisher = {
    new MessageBusPublisher {
      override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] = {
        Future.fromTry {
          Try {
            _receiver match {
              case Some(receiver) => receiver ! msg
              case None => _buffer.enqueue(msg)
            }
          }
        }
      }
    }
  }
}
