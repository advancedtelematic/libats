package com.advancedtelematic.metrics

import io.circe.Json

import scala.concurrent.Future

trait MetricsRepresentation {
  def metricsJson: Future[Json]

  def urlPrefix: String
}
