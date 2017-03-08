package com.advancedtelematic.libats.messaging

import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import shapeless.Lazy

import scala.reflect.ClassTag

object Messages {

  val partitionPrefixSize = 256

  implicit class StreamNameOp[T <: Class[_]](v: T) {
    def streamName: String = {
      v.getSimpleName.filterNot(c => List('$').contains(c))
    }
  }

  implicit class StreamNameInstanceOp[T](v: T)(implicit ev: MessageLike[T]) {
    def streamName: String = v.getClass.streamName
  }

  object MessageLike {
    def apply[T](idFn: T => String)
                (implicit ct: ClassTag[T],
                 encode: Lazy[DerivedObjectEncoder[T]],
                 decode: Lazy[DerivedDecoder[T]]): MessageLike[T] = new MessageLike[T] {
      override def id(v: T): String = idFn(v)

      import io.circe.generic.semiauto._

      override implicit val encoder: Encoder[T] = deriveEncoder[T]
      override implicit val decoder: Decoder[T] = deriveDecoder[T]
    }
  }

  abstract class MessageLike[T]()(implicit val tag: ClassTag[T]) {
    def streamName: String = tag.runtimeClass.streamName

    def id(v: T): String

    def partitionKey(v: T): String = id(v).take(partitionPrefixSize)

    def parse(json: String): io.circe.Error Either T = decode[T](json)

    implicit val encoder: Encoder[T]

    implicit val decoder: Decoder[T]
  }
}
