package com.advancedtelematic.libats.http.tracing

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directive1, Directives}
import com.advancedtelematic.libats.http.tracing.Tracing.{AkkaHttpClientTracing, ServerRequestTracing, Tracing}

import scala.concurrent.{ExecutionContext, Future}


class NullServerRequestTracing extends ServerRequestTracing  {
  override def newChild: ServerRequestTracing = this

  override def finishSpan: Unit = ()

  override def httpClientTracing(remoteServiceName: String): AkkaHttpClientTracing = new NullHttpClientTracing

  override def traceId: Long = 0l
}

class NullTracing extends Tracing {
  override def traceRequests: Directive1[ServerRequestTracing] = Directives.provide(new NullServerRequestTracing)

  override def shutdown: Unit = ()
}

class NullHttpClientTracing extends AkkaHttpClientTracing {
  override def trace(fn: HttpRequest => Future[HttpResponse])(implicit ec: ExecutionContext): HttpRequest => Future[HttpResponse] = fn
}
