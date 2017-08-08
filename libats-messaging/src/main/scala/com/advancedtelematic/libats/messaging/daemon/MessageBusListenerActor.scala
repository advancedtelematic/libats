package com.advancedtelematic.libats.messaging.daemon

import akka.{Done, NotUsed}
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.advancedtelematic.libats.messaging.daemon.MessageBusListenerActor.Subscribe

import scala.concurrent.duration._
import scala.util.Try
import akka.pattern.pipe
import com.advancedtelematic.libats.messaging.ListenerMonitor
import com.advancedtelematic.libats.messaging.Messages.MessageLike
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class MessageBusListenerActor[M](source: Source[M, NotUsed], monitor: ListenerMonitor, sink: Sink[M, Future[Done]])
                                (implicit messageLike: MessageLike[M])
  extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  override def postRestart(reason: Throwable): Unit = {
    log.error(reason, "Listener restarted, subscribing again")
    trySubscribeDelayed()
  }

  private def subscribed: Receive = {
    log.info(s"Subscribed to ${messageLike.streamName}")

    {
      case Subscribe =>
        log.warning("Listener already subscribed. Ignoring Subscribe message")
      case Failure(ex) =>
        log.error(ex, "Source/Listener died, subscribing again")
        monitor.onError(ex)
        trySubscribeDelayed()
        context become idle
      case Done =>
        monitor.onFinished
        log.info("Source finished, stopping message listener actor")
        context.stop(self)
    }
  }

  private def subscribe(): Unit = {
    log.info(s"Subscribing to ${messageLike.streamName}")

    source.mapAsync(1)(monitorSafe).runWith(sink).pipeTo(self)

    context become subscribed
  }

  private def monitorSafe: M => Future[M] = { msg =>
    monitor.onProcessed.map(_ => msg).recover {
      case tx =>
        log.warning(s"Could not monitor onProcessed state: ${tx.getMessage}")
        msg
    }
  }

  private def idle: Receive = {
    case Subscribe =>
      Try(subscribe()).failed.foreach { ex =>
        log.error(ex, "Could not subscribe, trying again")
        trySubscribeDelayed()
      }
  }

  override def receive: Receive = idle

  private def trySubscribeDelayed(delay: FiniteDuration = 5.seconds): Unit = {
    context.system.scheduler.scheduleOnce(delay, self, Subscribe)
  }
}

object MessageBusListenerActor {
  case object Subscribe

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  def loggingSink[M](implicit ml: MessageLike[M]): Sink[M, Future[Done]] = Sink.foreach[M] { msg =>
    log.info(s"Processed ${ml.streamName} - ${ml.id(msg)}")
  }

  def props[M](source: Source[M, NotUsed], monitor: ListenerMonitor)(implicit ml: MessageLike[M]): Props
  = Props(new MessageBusListenerActor[M](source, monitor, loggingSink))
}
