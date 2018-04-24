/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package com.advancedtelematic.libats.messaging

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import MessageListener.CommittableMsg
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.messaging.kafka.Commiter
import com.advancedtelematic.libats.messaging_datatype.MessageLike

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object LocalMessageBus {
  case class LocalBusCommittableMsg[T](_msg: T)(implicit val messageLike: MessageLike[T]) extends CommittableMsg[T] {
    override val msg: T = _msg
  }

  class LocalCommitter extends Commiter {
    override def commit(msg: CommittableMsg[_]): Future[Done] = FastFuture.successful(Done)

    override def commit(msgs: Seq[CommittableMsg[_]]): Future[Done] = FastFuture.successful(Done)
  }

  def subscribe[T](system: ActorSystem)(implicit m: MessageLike[T]): Source[T, NotUsed] = {
    Source.actorRef(MessageBus.DEFAULT_CLIENT_BUFFER_SIZE, OverflowStrategy.dropTail).mapMaterializedValue { ref =>
      system.eventStream.subscribe(ref, m.tag.runtimeClass)
      NotUsed
    }
  }

  def publisher(system: ActorSystem): MessageBusPublisher = {
    new MessageBusPublisher {
      override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] = {
        Future.fromTry(Try(system.eventStream.publish(msg.asInstanceOf[AnyRef])))
      }
    }
  }
}
