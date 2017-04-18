package com.advancedtelematic.metrics

import scala.util.{Failure, Success}

object AkkaHttpMetricsSink {

  def apply(settings: InfluxDbMetricsReporterSettings)(implicit system: ActorSystem,
                                                       mat: Materializer): Sink[String, _] = {
    val queryBuilder = Query.newBuilder
    queryBuilder += "db"        -> settings.database
    queryBuilder += "precision" -> "ms"
    settings.retentionPolicy.foreach(queryBuilder += "rp" -> _)
    val requestUri = Uri./.withPath(Path("/write")).withQuery(queryBuilder.result())
    val toRequest: String => HttpRequest = metrics => {

      import akka.http.scaladsl.client.RequestBuilding._
      settings.credentials.foldLeft(
        Post(
          requestUri,
          HttpEntity(
            MediaTypes.`application/x-www-form-urlencoded` withCharset HttpCharsets.`UTF-8`,
            metrics
          )
        )
      )((r, c) => r.addHeader(Authorization(BasicHttpCredentials(c.username, c.password))))
    }

    val httpFlow = Http(system).newHostConnectionPool[NotUsed](
      host = settings.host,
      port = settings.port,
      settings = ConnectionPoolSettings(system).withMaxConnections(1)
    )
    val log = Logging(system, this.getClass)
    Flow[String]
      .log("metrics")(log)
      .map(toRequest)
      .map(_ -> NotUsed)
      .via(httpFlow)
      .map(_._1)
      .to(Sink.foreach {
        case Success(r @ HttpResponse(StatusCodes.NoContent, _, _, _)) =>
          log.info("Metrics reported.")
          r.entity.discardBytes()

        case Success(r @ HttpResponse(status, _, entity, _)) =>
          log.warning("InfluxDB responded with '{}'", r)
          entity.discardBytes()

        case Failure(t) =>
          log.error(t, "Failed to report metrics to InfluxDb")
      })
  }

}
