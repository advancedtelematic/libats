package com.advancedtelematic.libats.messaging.kafka

import akka.Done
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import com.advancedtelematic.libats.messaging.MessageListener.{CommittableMsg, KafkaMsg}

import scala.concurrent.Future

trait Commiter {
  def commit(msg: CommittableMsg[_]): Future[Done]

  def commit(msgs: Seq[CommittableMsg[_]]): Future[Done]
}

class KafkaCommiter extends Commiter {
  override def commit(msg: CommittableMsg[_]): Future[Done] = msg match {
    case kafkaMsg: KafkaMsg[_] =>
      kafkaMsg.offset.commitScaladsl()
    case _ =>
      Future.failed(new RuntimeException("fatal error: Kafka committer can only commit KafkaMsg"))
  }

  override def commit(msgs: Seq[CommittableMsg[_]]): Future[Done] = {
    val offset = msgs.foldRight(CommittableOffsetBatch.empty) {
      case (kafkaMsg: KafkaMsg[_], acc) =>
        acc.updated(kafkaMsg.offset)
      case _ =>
        throw new RuntimeException("fatal error: Kafka committer can only commit KafkaMsg")
    }

    offset.commitScaladsl()
  }
}
