package com.advancedtelematic.metrics

import akka.http.scaladsl.server.Directive0
import com.codahale.metrics.MetricRegistry

trait AkkaHttpRequestMetrics {
  import akka.http.scaladsl.server.Directives._

  def requestMetrics(registry: MetricRegistry): Directive0 = {
    val requests = registry.counter("http_requests")
    val success = registry.counter("http_requests_success")
    val failures_4xx = registry.counter("http_requests_failed_4xx")
    val failures_5xx = registry.counter("http_requests_failed_5xx")
    val responseTime = registry.histogram("http_app_response_time")

    extractRequestContext flatMap { ctx =>
      val startAt = System.currentTimeMillis()

      mapResponse { resp =>
        requests.inc()

        if(resp.status.isSuccess())
          success.inc()
        else if(resp.status.intValue() >= 500)
          failures_5xx.inc()
        else
          failures_4xx.inc()

        responseTime.update(System.currentTimeMillis() - startAt)

        resp
      }
    }
  }
}

object AkkaHttpRequestMetrics extends AkkaHttpRequestMetrics
