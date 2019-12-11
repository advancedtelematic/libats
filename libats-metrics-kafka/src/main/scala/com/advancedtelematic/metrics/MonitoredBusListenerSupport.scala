package com.advancedtelematic.metrics

import akka.actor.ActorRef
import com.advancedtelematic.libats.http.BootApp
import com.advancedtelematic.libats.messaging.MessageListenerSupport
import com.advancedtelematic.libats.messaging.MsgOperation.MsgOperation
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import io.prometheus.client.CollectorRegistry

trait MonitoredBusListenerSupport {
  self: BootApp with MessageListenerSupport =>

  def startMonitoredListener[T : MessageLike](op: MsgOperation[T], registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
                                              skipProcessingErrors: Boolean = false): ActorRef = {
    startListener(op, PrometheusMessagingMonitor[T](), skipProcessingErrors)
  }
}
