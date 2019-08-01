/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package com.advancedtelematic.libats.messaging

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.advancedtelematic.libats.messaging.MsgOperation.MsgOperation
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object LocalMessageBus {

  def subscribe[T](system: ActorSystem, config: Config, op: MsgOperation[T])(implicit ec: ExecutionContext, m: MessageLike[T]): Source[T, NotUsed] = {
    val handlerParallelism = config.getInt("messaging.listener.parallelism")
    Source.actorRef[T](MessageBus.DEFAULT_CLIENT_BUFFER_SIZE, OverflowStrategy.dropTail).mapMaterializedValue { ref =>
      system.eventStream.subscribe(ref, m.tag.runtimeClass)
      NotUsed
    }.mapAsync(handlerParallelism){ x: T => op.apply(x).map(_ => x) }
  }

  def publisher(system: ActorSystem): MessageBusPublisher = {
    new MessageBusPublisher {
      override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] = {
        Future.fromTry(Try(system.eventStream.publish(msg.asInstanceOf[AnyRef])))
      }
    }
  }
}
