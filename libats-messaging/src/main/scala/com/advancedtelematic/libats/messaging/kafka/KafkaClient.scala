/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package com.advancedtelematic.libats.messaging.kafka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscription, Subscriptions}
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import io.circe.syntax._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization._
import com.advancedtelematic.libats.messaging.MessageBusPublisher
import com.advancedtelematic.libats.messaging_datatype.MessageLike

import scala.concurrent.{ExecutionContext, Future, Promise}

object KafkaClient {

  def publisher(system: ActorSystem, config: Config): MessageBusPublisher = {
    val cfg = config.getConfig("messaging.kafka")
    val topicNameFn = topic(cfg)
    val kafkaProducer = producer(cfg)(system)

    new MessageBusPublisher {
      override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] = {
        val promise = Promise[RecordMetadata]()

        val topic = topicNameFn(messageLike.streamName)

        val record = new ProducerRecord[Array[Byte], String](topic,
          messageLike.id(msg).getBytes, msg.asJson(messageLike.encoder).noSpaces)

        kafkaProducer.send(record, new Callback {
          override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
            if (exception != null)
              promise.failure(exception)
            else if (metadata != null)
              promise.success(metadata)
            else
              promise.failure(new Exception("Unknown error occurred, no metadata or error received"))
          }
        })

        promise.future.map(_ => ())
      }
    }
  }


  def source[T](system: ActorSystem, config: Config)
               (implicit ml: MessageLike[T]): Source[T, NotUsed] =
    plainSource(config)(ml, system)

  private def plainSource[T](config: Config)
                            (implicit ml: MessageLike[T], system: ActorSystem): Source[T, NotUsed] = {
    buildSource(config) { (cfgSettings: ConsumerSettings[Array[Byte], T], subscriptions) =>
      val settings = cfgSettings.withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
      Consumer.plainSource(settings, subscriptions).map(_.value()).filter(_ != null)
    }
  }

  def committableSource[T](config: Config)
                          (implicit ml: MessageLike[T], system: ActorSystem): Source[CommittableMessage[Array[Byte], T], NotUsed] =
    buildSource(config) { (cfgSettings: ConsumerSettings[Array[Byte], T], subscriptions) =>
      Consumer.committableSource(cfgSettings, subscriptions).filter(_.record.value() != null)
    }

  private def consumerSettings[T](system: ActorSystem, config: Config)
                                 (implicit ml: MessageLike[T]): ConsumerSettings[Array[Byte], T] = {
    val host = config.getString("host")
    val topicFn = topic(config)
    val groupId = config.getString("groupIdPrefix") + "-" + topicFn(ml.streamName)

    ConsumerSettings(system, new ByteArrayDeserializer, new JsonDeserializer(ml.decoder))
      .withBootstrapServers(host)
      .withGroupId(groupId)
      .withClientId(s"consumer-$groupId")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
  }

  private def buildSource[T, M](config: Config)
                               (consumerFn: (ConsumerSettings[Array[Byte], M], Subscription) => Source[T, Control])
                               (implicit system: ActorSystem, ml: MessageLike[M]): Source[T, NotUsed] = {
    val cfg = config.getConfig("messaging.kafka")
    val cfgSettings = consumerSettings(system, cfg)
    val topicFn = topic(cfg)
    val subscription = Subscriptions.topics(topicFn(ml.streamName))

    consumerFn(cfgSettings, subscription).mapMaterializedValue { _ => NotUsed }
  }

  private[this] def topic(config: Config): String => String = {
    val suffix = config.getString("topicSuffix")
    (streamName: String) => streamName + "-" + suffix
  }

  private[this] def producer(config: Config)
                            (implicit system: ActorSystem): KafkaProducer[Array[Byte], String] =
    ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(config.getString("host"))
      .createKafkaProducer()
}
