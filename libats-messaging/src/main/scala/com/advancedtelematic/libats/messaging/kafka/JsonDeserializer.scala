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
