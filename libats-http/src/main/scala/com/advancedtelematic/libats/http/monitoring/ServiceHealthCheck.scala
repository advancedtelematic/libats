package com.advancedtelematic.libats.http.monitoring

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.stream.Materializer
import com.advancedtelematic.libats.http.HealthCheck
import com.advancedtelematic.libats.http.HealthCheck.{Down, HealthCheckResult, Up}
import io.circe.syntax._
import cats.syntax.either._

import scala.concurrent.{ExecutionContext, Future}

class ServiceHealthCheck(address: Uri)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext) extends HealthCheckHttpClient with HealthCheck {
  override def apply(logger: LoggingAdapter)(implicit ec: ExecutionContext): Future[HealthCheckResult] = {
    val req = HttpRequest(HttpMethods.GET, address.withPath(Path("/health")))
    execute(req)
      .map(_ => Up)
      .recover {
        case ex =>
          logger.error(ex, s"service $address is down")
          Down(ex)
      }
  }

  override def name: String = address.toString()
}
