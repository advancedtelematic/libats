/*
 * Copyright 2016 ATS Advanced Telematic Systems GmbH
 */
package com.advancedtelematic.metrics

import java.time.Instant

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.codahale.metrics._
import io.circe.Decoder

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.collection.JavaConverters._

final case class UsernamePassword(username: String, password: String)

object UsernamePassword {
  implicit val DecoderInstance: Decoder[UsernamePassword] = io.circe.generic.semiauto.deriveDecoder
}

final case class InfluxDbMetricsReporterSettings(host: String,
                                                 port: Int,
                                                 database: String,
                                                 serviceName: String,
                                                 instanceId: String,
                                                 interval: FiniteDuration,
                                                 credentials: Option[UsernamePassword],
                                                 retentionPolicy: Option[String])

object InfluxDbMetricsReporter {

  case object Tick

  private def appendNameAndTags(metric: Metric, registeredName: String)(sb: StringBuilder): StringBuilder =
    metric match {
      case NamedMetric(_, name, tags) =>
        sb.append(name)
        for (x <- tags)
          sb.append(",").append(x._1).append("=").append(x._2)
        sb
      case _ =>
        sb.append(registeredName)
    }

  private def formatValue(value: Any): String = value match {
    case x: Double =>
      if (x.isInfinite || x.isNaN) "0.0" else x.toString

    case x: java.util.Collection[_] =>
      s""""${x.toString}""""

    case _ => value.toString
  }

  private def appendMetered(metric: Metered)(sb: StringBuilder): StringBuilder = {
    sb.append("count=").append(metric.getCount)
    sb.append(",rateMean=").append(metric.getMeanRate)
    sb.append(",rate1=").append(metric.getOneMinuteRate)
    sb.append(",rate5=").append(metric.getFiveMinuteRate)
    sb.append(",rate15=").append(metric.getFifteenMinuteRate)
  }

  private def appendSnapshot(s: Snapshot)(sb: StringBuilder): StringBuilder = {
    sb.append("p50=").append(s.getMedian)
    sb.append(",p75=").append(s.get75thPercentile())
    sb.append(",p95=").append(s.get98thPercentile())
    sb.append(",p98=").append(s.get98thPercentile())
    sb.append(",p99=").append(s.get99thPercentile())
    sb.append(",p999=").append(s.get999thPercentile())
    sb.append(",p9999=").append(s.getValue(0.9999))
    sb.append(",min=").append(s.getMin)
    sb.append(",max=").append(s.getMax)
  }

  private def `,`(sb: StringBuilder): StringBuilder = sb.append(",")

  private def appendMetricValues(metric: Metric)(sb: StringBuilder): StringBuilder = metric match {
    case x: NamedMetric[_] => appendMetricValues(x.metric)(sb)
    case x: Counter        => sb.append("value=").append(x.getCount)
    case x: Gauge[_] =>
      sb.append("value=").append(formatValue(x.getValue))
    case x: Histogram =>
      appendSnapshot(x.getSnapshot)(sb)
    case x: Timer =>
      (appendMetered(x) _ andThen `,` andThen appendSnapshot(x.getSnapshot))(sb)
    case x: Metered =>
      appendMetered(x)(sb)
  }

  def formatMetrics(registry: MetricRegistry, appendCommonTags: StringBuilder => StringBuilder): String = {
    val metrics   = registry.getMetrics.asScala
    val builder   = new StringBuilder
    val timestamp = Instant.now().toEpochMilli.toString
    metrics.foldLeft(builder) { (b, e) =>
      (appendNameAndTags(e._2, e._1) _ andThen appendCommonTags andThen appendMetricValues(e._2))(b)
        .append(" ")
        .append(timestamp)
        .append("\n")
    }
    builder.toString()
  }

  def start(settings: InfluxDbMetricsReporterSettings, registry: MetricRegistry, influxSink: Sink[String, _])(
      implicit materializer: Materializer
  ) = {
    val tags             = s",service=${settings.serviceName},hostname=${settings.instanceId} "
    val appendCommonTags = (sb: StringBuilder) => sb.append(tags)
    Source
      .tick(Duration.Zero, settings.interval, Tick)
      .map(_ => formatMetrics(registry, appendCommonTags))
      .to(influxSink)
      .run()
  }
}
