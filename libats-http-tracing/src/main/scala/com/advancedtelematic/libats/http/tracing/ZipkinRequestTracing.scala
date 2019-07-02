package com.advancedtelematic.libats.http.tracing

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directive1
import brave.http.{HttpServerAdapter, HttpServerHandler, HttpTracing}
import brave.propagation.TraceContext
import brave.{Span, Tracing => BraveTracing}
import com.advancedtelematic.libats.http.tracing.Tracing.{RequestTracing, Tracing}
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender

import scala.collection.concurrent.TrieMap


object ZipkinRequestTracing {
  def apply(uri: Uri, serviceName: String): ZipkinTracing = {
      val sender = OkHttpSender.create(uri + "/api/v2/spans")
      val spanReporter = AsyncReporter.create(sender)

      val tracing = BraveTracing.newBuilder.localServiceName(serviceName).spanReporter(spanReporter).build

      val httpTracing = HttpTracing.newBuilder(tracing).build()

      new ZipkinTracing(httpTracing)
  }
}

class ZipkinTracing(httpTracing: HttpTracing) extends Tracing {
  import akka.http.scaladsl.server.Directives._

  private def createAkkaHandler(tracing: HttpTracing)  =
    HttpServerHandler.create(tracing, new ZipkinTracingHttpAdapter)

  private def extractor(tracing: HttpTracing): TraceContext.Extractor[HttpRequest] =
    tracing.tracing.propagation.extractor((carrier: HttpRequest, key: String) => carrier.headers.find(_.name() == key).map(_.value()).orNull)

  private def traceRequest(req: HttpRequest): Boolean =
    req.uri.path.startsWith(Uri.Path("/health")) == false

  private def filterHeader(name: String, value: String): Option[(String, String)] = {
    if (name.toLowerCase == "authorization")
      Option(name -> "<removed>")
    else if (name.toLowerCase == "timeout-access")
      None
    else
      Option(name -> value)
  }

  override def traceRequests: Directive1[RequestTracing] = extractRequest.flatMap {
    case req if traceRequest(req) =>
      val span = createAkkaHandler(httpTracing).handleReceive(extractor(httpTracing), req)

      req.headers
        .flatMap(h => filterHeader(h.name(), h.value()))
        .foreach { case (name, value) =>
          span.tag(name, value)
        }

      mapResponse { resp =>
        val headers = TrieMap.empty[String, String]

        httpTracing.tracing().propagation
          .injector((_: HttpResponse, key: String, value: String) => headers.put(key, value))
          .inject(span.context(), resp)

        val respWithHeaders = headers.foldLeft(resp) { case (acc, (k, v)) =>
          acc.addHeader(RawHeader(k, v))
        }

        createAkkaHandler(httpTracing).handleSend(respWithHeaders, null, span)

        respWithHeaders

      }.tflatMap(_ => provide(new ZipkinRequestTracing(httpTracing, span)))
    case _ =>
      provide(new NullRequestTracing)
  }

  override def shutdown: Unit = {
    httpTracing.tracing().close()
  }
}

class ZipkinRequestTracing(httpTracing: HttpTracing, requestSpan: Span) extends RequestTracing {
  def newChild: ZipkinRequestTracing = {
    val child = httpTracing.tracing().tracer().newChild(requestSpan.context()).start()
    new ZipkinRequestTracing(httpTracing, child)
  }

  def headers(request: HttpRequest): Map[String, String] = {
    val headers = TrieMap.empty[String, String]

    httpTracing.tracing().propagation
      .injector((_: HttpRequest, key: String, value: String) => headers.put(key, value))
      .inject(requestSpan.context(), request)

    headers.toMap
  }

  def finishSpan(): Unit = requestSpan.finish()
}

protected class ZipkinTracingHttpAdapter extends HttpServerAdapter[HttpRequest, HttpResponse] {
  override def method(request: HttpRequest): String =
    request.method.value

  override def url(request: HttpRequest): String =
    request.uri.toString()

  override def requestHeader(request: HttpRequest, name: String): String =
    request.headers.find(_.name() == name).map(_.value()).orNull

  override def statusCode(response: HttpResponse): Integer =
    response.status.intValue()
}
