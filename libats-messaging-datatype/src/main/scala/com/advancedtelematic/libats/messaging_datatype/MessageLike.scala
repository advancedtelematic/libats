package com.advancedtelematic.libats.messaging_datatype

import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedAsObjectEncoder
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import shapeless.Lazy

import scala.reflect.ClassTag

object MessageLike {
  val partitionPrefixSize = 256

  implicit class StreamNameOp[T <: Class[_]](v: T) {
    def streamName: String = {
      v.getSimpleName.filterNot(_ == '$')
    }
  }

  implicit class StreamNameInstanceOp[T](v: T)(implicit ev: MessageLike[T]) {
    def streamName: String = v.getClass.streamName
  }

  def apply[T](idFn: T => String)
              (implicit ct: ClassTag[T],
               encoderInstance: Encoder[T],
               decoderInstance: Decoder[T]): MessageLike[T] = new MessageLike[T] {
    override def id(v: T): String = idFn(v)

    override implicit val encoder: Encoder[T] = encoderInstance
    override implicit val decoder: Decoder[T] = decoderInstance
  }

  def derive[T](idFn: T => String)(implicit ct: ClassTag[T],
                                   encode: Lazy[DerivedAsObjectEncoder[T]],
                                   decode: Lazy[DerivedDecoder[T]]): MessageLike[T] = new MessageLike[T] {
      override def id(v: T): String = idFn(v)

      override implicit val encoder: Encoder[T] = deriveEncoder
      override implicit val decoder: Decoder[T] = deriveDecoder
    }
}

abstract class MessageLike[T]()(implicit val tag: ClassTag[T]) {
  import MessageLike._

  def streamName: String = tag.runtimeClass.streamName

  def id(v: T): String

  def partitionKey(v: T): String = id(v).take(partitionPrefixSize)

  def parse(json: String): io.circe.Error Either T = decode[T](json)

  implicit val encoder: Encoder[T]

  implicit val decoder: Decoder[T]
}
