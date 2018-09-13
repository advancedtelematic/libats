package com.advancedtelematic.libats.http.logging

import akka.actor.{Actor, ActorSystem, DiagnosticActorLogging, Props, SupervisorStrategy}
import akka.event.Logging
import akka.event.Logging.MDC
import akka.routing.RoundRobinPool
import com.advancedtelematic.libats.http.logging.RequestLoggingActor.LogMsg
import com.typesafe.config.ConfigFactory

import scala.util.Random

object RequestLoggingActor {
  case class LogMsg(formattedMsg: String, metrics: Map[String, String])

  private val config = ConfigFactory.load()

  def router(level: Logging.LogLevel)(implicit system: ActorSystem): Props = {
    val restartStrategy = SupervisorStrategy.defaultStrategy
    val childCount = config.getInt("ats.http.logging.router.childCount")
    RoundRobinPool(childCount, supervisorStrategy = restartStrategy).props(props(level))
  }

  def props(level: Logging.LogLevel) = Props(new RequestLoggingActor(level))
}

class RequestLoggingActor(level: Logging.LogLevel) extends Actor with DiagnosticActorLogging {
  override def mdc(currentMessage: Any): MDC = currentMessage match {
    case LogMsg(_, metrics) =>
      metrics
    case _ =>
      Logging.emptyMDC
  }

  override def receive: Receive = {
    case LogMsg(formattedMsg, _) =>
      log.log(level, formattedMsg)
  }
}
