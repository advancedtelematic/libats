package com.advancedtelematic.libats.messaging.test

import java.util.concurrent.ConcurrentHashMap

import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.messaging.MessageBusPublisher
import com.advancedtelematic.libats.messaging_datatype.MessageLike

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class MockMessageBus extends MessageBusPublisher {

  private case class ReceivedMsg(tag: ClassTag[_], payload: Any)

  private val received = new ConcurrentHashMap[String, ReceivedMsg]()

  def findReceived[T](filterFn: T => Boolean)(implicit msgLike: MessageLike[T]): Option[T] = {
    val found: T = received.searchValues(1024, (msg: ReceivedMsg) => {
      val maybeT = if(msg.tag == msgLike.tag) Some(msg.payload.asInstanceOf[T]) else None

      maybeT match {
        case Some(t) if filterFn(t) => t
        case _ => null.asInstanceOf[T]
      }
    })

    Option(found)
  }

  def findReceived[T](id: String)(implicit msgLike: MessageLike[T]): Option[T] =
    findReceived[T]((t: T) => msgLike.id(t) == id)

  def reset(): Unit =
    received.clear()

  override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] = {
    received.put(messageLike.id(msg), ReceivedMsg(messageLike.tag, msg))
    FastFuture.successful(())
  }
}
