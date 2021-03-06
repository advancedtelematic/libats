package com.advancedtelematic.libats.http.tracing

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.util.FastFuture
import brave.Span
import brave.http.{HttpClientAdapter, HttpTracing}
import com.advancedtelematic.libats.http.tracing.Tracing.AkkaHttpClientTracing

import scala.concurrent.{ExecutionContext, Future}

class ZipkinAkkaHttpClientTracing(httpTracing: HttpTracing, currentSpan: Span, serverName: String) extends AkkaHttpClientTracing {
  private class ZipkinHttpClientAdapter extends HttpClientAdapter[HttpRequest, HttpResponse] with AkkaHttpTracingAdapter

  private def injectTracingHeaders(httpTracing: HttpTracing, span: Span, req: HttpRequest): HttpRequest = {
    var tracedReq = req

    httpTracing.tracing()
      .propagation()
      .injector((_: HttpRequest, key: String, value: String) => tracedReq = tracedReq.addHeader(RawHeader(key, value)))
      .inject(span.context(), req)

    tracedReq
  }

  override def trace(fn: HttpRequest => Future[HttpResponse])(implicit ec: ExecutionContext): HttpRequest => Future[HttpResponse] = (req: HttpRequest) => {
    val tracing = httpTracing.tracing()

    val newSpan = Option(currentSpan.context())
      .map(tracing.tracer().newChild)
      .getOrElse(tracing.tracer().withSampler(tracing.sampler()).newTrace())
      .kind(Span.Kind.CLIENT)
      .remoteServiceName(serverName)
      .tag("http.method", req.method.value)
      .tag("http.uri", req.uri.toString())
      .annotate(s"$serverName-client-send")
      .start()

    val tracedReq = injectTracingHeaders(httpTracing, newSpan, req)

    fn(tracedReq).map { resp =>
      newSpan
        .tag("http.response_code", resp.status.intValue().toString)
        .annotate(s"$serverName-client-receive")
        .finish()
      resp
    }.recoverWith {
      case ex =>
        newSpan
          .annotate(s"$serverName-client-receive")
          .error(ex)
        FastFuture.failed(ex)
    }
  }
}
