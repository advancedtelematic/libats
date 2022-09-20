/*
 * Copyright (C) 2016 HERE Global B.V.
 *
 * Licensed under the Mozilla Public License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.mozilla.org/en-US/MPL/2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: MPL-2.0
 * License-Filename: LICENSE
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
