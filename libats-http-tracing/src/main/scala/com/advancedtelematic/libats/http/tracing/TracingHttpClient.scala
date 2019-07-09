package com.advancedtelematic.libats.http.tracing

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.advancedtelematic.libats.http.ServiceHttpClient
import com.advancedtelematic.libats.http.tracing.Tracing.ServerRequestTracing

import scala.concurrent.Future

abstract class TracingHttpClient(_httpClient: HttpRequest => Future[HttpResponse], remoteServiceName: String)
                                (implicit system: ActorSystem, mat: Materializer, serverTracing: ServerRequestTracing) extends ServiceHttpClient(_httpClient) {

  import system.dispatcher

  override def httpClient: HttpRequest => Future[HttpResponse] =
    serverTracing.httpClientTracing(remoteServiceName).trace(super.httpClient)
}
