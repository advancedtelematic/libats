package com.advancedtelematic.libats.http.tracing

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directive1
import brave.http.{HttpServerAdapter, HttpServerHandler, HttpTracing}
import brave.propagation.TraceContext
import brave.{Span, Tracing => BraveTracing}
import com.advancedtelematic.libats.http.tracing.Tracing.{AkkaHttpClientTracing, ServerRequestTracing, Tracing}
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender

import scala.collection.concurrent.TrieMap


class ZipkinTracing(httpTracing: HttpTracing) extends Tracing {
  import akka.http.scaladsl.server.Directives._

  private class ZipkinTracingHttpAdapter extends HttpServerAdapter[HttpRequest, HttpResponse]with AkkaHttpTracingAdapter

  private def createAkkaHandler(tracing: HttpTracing)  =
    HttpServerHandler.create(tracing, new ZipkinTracingHttpAdapter)

  private def extractor(tracing: HttpTracing): TraceContext.Extractor[HttpRequest] =
    tracing.tracing.propagation.extractor((carrier: HttpRequest, key: String) => carrier.headers.find(_.name() == key).map(_.value()).orNull)

  private def traceRequest(req: HttpRequest): Boolean = {
    List("/health", "/metrics").forall { p =>
      req.uri.path.startsWith(Uri.Path(p)) == false
    }
  }

  private def filterHeader(name: String, value: String): Option[(String, String)] = {
    if (name.toLowerCase == "authorization")
      Option(name -> "<removed>")
    else if (name.toLowerCase == "timeout-access")
      None
    else
      Option(name -> value)
  }

  lazy val handler = createAkkaHandler(httpTracing)

  override def traceRequests: Directive1[ServerRequestTracing] = extractRequest.flatMap {
    case req if traceRequest(req) =>
      val span = handler.handleReceive(extractor(httpTracing), req)

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

        handler.handleSend(respWithHeaders, null, span)

        respWithHeaders

      }.tflatMap(_ => provide(new ZipkinServerRequestTracing(httpTracing, span)))
    case _ =>
      provide(new NullServerRequestTracing)
  }

  override def shutdown: Unit = {
    httpTracing.tracing().close()
  }
}

object ZipkinServerRequestTracing {
  def apply(uri: Uri, serviceName: String): ZipkinTracing = {
    val sender = OkHttpSender.create(uri + "/api/v2/spans")
    val spanReporter = AsyncReporter.create(sender)

    val tracing = BraveTracing.newBuilder
      .localServiceName(serviceName)
      .spanReporter(spanReporter).build

    val httpTracing = HttpTracing.newBuilder(tracing).build()

    new ZipkinTracing(httpTracing)
  }
}

class ZipkinServerRequestTracing(httpTracing: HttpTracing, requestSpan: Span) extends ServerRequestTracing {
  override def newChild: ZipkinServerRequestTracing = {
    val child = httpTracing.tracing().tracer().newChild(requestSpan.context()).start()
    new ZipkinServerRequestTracing(httpTracing, child)
  }

  override def finishSpan(): Unit = requestSpan.finish()

  override def httpClientTracing(remoteServiceName: String): AkkaHttpClientTracing = {
    new ZipkinAkkaHttpClientTracing(httpTracing, requestSpan, remoteServiceName)
  }

  override def traceId: Long = requestSpan.context().traceId()
}
