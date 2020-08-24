package com.advancedtelematic.libats.messaging.test

import com.advancedtelematic.libats.messaging.MessageBusPublisher
import com.advancedtelematic.libats.messaging_datatype.MessageLike

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class MockMessageBus extends MessageBusPublisher {

  private case class ReceivedMsg(tag: ClassTag[_], payload: Any)

  private var received = Seq.empty[ReceivedMsg]

  def findReceivedAll[T](filterFn: T => Boolean)(implicit msgLike: MessageLike[T]): Seq[T] =
    received
        .filter(_.tag == msgLike.tag)
        .map(_.payload.asInstanceOf[T])
        .filter(filterFn)

  def findReceivedAll[T](id: String)(implicit msgLike: MessageLike[T]): Seq[T] =
    findReceivedAll[T]((t: T) => msgLike.id(t) == id)

  def findReceived[T](filterFn: T => Boolean)(implicit msgLike: MessageLike[T]): Option[T] =
    findReceivedAll(filterFn).headOption

  def findReceived[T](id: String)(implicit msgLike: MessageLike[T]): Option[T] = {
    findReceived[T]((t: T) => msgLike.id(t) == id)
  }

  def reset(): Unit =
    received = Seq.empty[ReceivedMsg]

  override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] = {
    received = ReceivedMsg(messageLike.tag, msg) +: received
    Future.unit
  }
}
