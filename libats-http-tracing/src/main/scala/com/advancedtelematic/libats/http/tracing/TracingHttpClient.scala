package com.advancedtelematic.libats.http.tracing

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import com.advancedtelematic.libats.http.Errors.RemoteServiceError
import com.advancedtelematic.libats.http.ServiceHttpClient
import com.advancedtelematic.libats.http.tracing.Tracing.RequestTracing

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

abstract class TracingHttpClient(httpClient: HttpRequest => Future[HttpResponse])
                                (implicit system: ActorSystem, mat: Materializer, tracing: RequestTracing) extends ServiceHttpClient(httpClient) {
  import system.dispatcher

  override protected def execHttp[T](request: HttpRequest)(errorHandler: PartialFunction[RemoteServiceError, Future[T]])
                                    (implicit ev: ClassTag[T], um: FromEntityUnmarshaller[T]): Future[T] = {
    val childTracing = tracing.newChild

    val requestWithHeaders = childTracing.headers(request).foldLeft(request) { case (acc, (k, v)) =>
      acc.addHeader(RawHeader(k, v))
    }

    super.execHttp(requestWithHeaders)(errorHandler)
      .map { r => Try(childTracing.finishSpan) ; r }
      .recoverWith { case ex => Try(childTracing.finishSpan) ; FastFuture.failed(ex) }
  }
}
