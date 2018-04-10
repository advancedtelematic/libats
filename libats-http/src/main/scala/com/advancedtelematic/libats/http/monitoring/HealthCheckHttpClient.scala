package com.advancedtelematic.libats.http.monitoring

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.Materializer
import io.circe.Json

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.control.NoStackTrace
import io.circe.syntax._
import cats.syntax.either._

abstract class HealthCheckHttpClient(implicit val system: ActorSystem, mat: Materializer, ec: ExecutionContext) {
  import HealthCheckHttpClient._

  lazy val _http = Http()

  protected def execute(request: HttpRequest): Future[Json] =
    _http.singleRequest(request).flatMap {
      case r @ HttpResponse(status, _, _, _) if status.isSuccess() =>
        jsonUnmarshaller(r.entity)
      case r =>
        Future.failed(HealthCheckError(s"Unexpected response from service: $r"))
    }

  private val jsonUnmarshaller =
    Unmarshaller
      .stringUnmarshaller
      .forContentTypes(ContentTypes.`application/json`)
      .map { data => io.circe.parser.parse(data).valueOr(throw _) }
}

object HealthCheckHttpClient {
  case class HealthCheckError(str: String) extends Throwable(str) with NoStackTrace
}
