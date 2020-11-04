/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package com.advancedtelematic.libats.messaging.kafka

import java.nio.ByteBuffer
import java.util

import cats.syntax.either._
import io.circe.Decoder
import io.circe.jawn._
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory

import scala.util.control.NoStackTrace

class JsonDeserializerException(msg: String, cause: Throwable) extends Exception(msg, cause) with NoStackTrace

class JsonDeserializer[T](decoder: Decoder[T], throwException: Boolean = false) extends Deserializer[T] {
  private lazy val _logger = LoggerFactory.getLogger(this.getClass)


  override def deserialize(topic: String, data: Array[Byte]): T = {

    def processError(msg: String, exception: Exception) = {
      if (throwException) {
        throw new JsonDeserializerException(msg, exception)
      } else {
        _logger.error(msg, exception)
        null.asInstanceOf[T]
      }
    }

    val buffer = ByteBuffer.wrap(data)

    parseByteBuffer(buffer) match {
      case Right(json) => json.as[T](decoder).fold(processError(s"Could not decode ${json.noSpaces} from $topic", _), identity)
      case Left(ex) => processError(s"Could not parse msg from $topic", ex)
    }
  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()

  override def close(): Unit = ()
}
