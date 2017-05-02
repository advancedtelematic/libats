/*
 * Copyright 2016 ATS Advanced Telematic Systems GmbH
 */
package com.advancedtelematic.metrics

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Materializer
import akka.stream.Supervision.resumingDecider
import akka.stream.scaladsl.{Flow, Sink}
import com.advancedtelematic.uri.Uri
import com.advancedtelematic.uri.Uri.{Path, Query}
import com.twitter.finagle.Http
import com.twitter.finagle.http._
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.util.{Base64StringEncoder, Return, Throw, Future => TwitterFuture, Promise => TwitterPromise}

import scala.concurrent.{ExecutionContext, Future => ScalaFuture, Promise => ScalaPromise}

object FinagleMetricsSink {

  implicit class RichTwitterFuture[A](val tf: TwitterFuture[A]) extends AnyVal {
    def asScala(implicit e: ExecutionContext): ScalaFuture[A] = {
      val promise: ScalaPromise[A] = ScalaPromise()
      tf.respond {
        case Return(value)    => promise.success(value)
        case Throw(exception) => promise.failure(exception)
      }
      promise.future
    }
  }

  def apply(settings: InfluxDbMetricsReporterSettings)(implicit system: ActorSystem,
                                                       mat: Materializer, executionContext: ExecutionContext): Sink[String, _] = {

    val client = Http.client
      .withLabel("influxdb")
      .withSessionPool
      .minSize(1)
      .withSessionPool
      .maxSize(2)
      .withSessionPool
      .maxWaiters(1)
      .withTracer(NullTracer)
      .withStatsReceiver(NullStatsReceiver)
      .newService(settings.host+":"+settings.port)


    val queryBuilder = Query.newBuilder
    queryBuilder += "db"        -> settings.database
    queryBuilder += "precision" -> "ms"
    settings.retentionPolicy.foreach(queryBuilder += "rp" -> _)


    val requestUri =
      Uri.Empty.withPath(Path("/write"))
        .withQuery(queryBuilder.result())
    val auth =
      settings.credentials.map(
        c => "Basic " + Base64StringEncoder.encode(s"${c.username}:${c.password}".getBytes("UTF-8"))
      )

    val toRequest : String => Request = metrics => {
      val req =Request(Method.Post, requestUri.toString())
      req.host = settings.host
      req.contentType = MediaType.WwwForm
      req.contentString = metrics
      auth.foreach(req.authorization_=)
      req
    }

    val log = Logging(system, this.getClass)

    Flow[String]
      .log("metrics")(log)
      .map(toRequest)
      .mapAsync(1)(client.apply(_).asScala)
      .withAttributes(supervisionStrategy(resumingDecider))
      .to(Sink.foreach {
        case response if response.status != Status.NoContent =>
          log.warning("Unexpected response from InfluxDb: %s\n%s", response, response.contentString)
        case _ => log.debug("metric reported ")
      })
  }
}
