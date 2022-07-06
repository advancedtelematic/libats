package com.advancedtelematic.libats.http

import akka.http.scaladsl.unmarshalling.{PredefinedFromStringUnmarshallers, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.data.{Limit, Offset}

object FromLongUnmarshallers {
  private val DefaultLimit = 1000

  implicit val offsetUnmarshaller: Unmarshaller[String, Offset] = createUnmarshaller(_ >= 0, Offset.apply, "Offset cannot be negative")

  def getLimitUnmarshaller(maxLimit: Long = DefaultLimit): Unmarshaller[String, Limit] =
    createUnmarshaller(l => l >= 0 && l <= maxLimit, Limit.apply, s"Limit cannot be negative or greater than $maxLimit")

  private def createUnmarshaller[T](isValidParam: Long => Boolean, createParam: Long => T, errorMessage: String): Unmarshaller[String, T] =
    PredefinedFromStringUnmarshallers.longFromStringUnmarshaller.flatMap { _ => _ => value =>
      if (isValidParam(value)) FastFuture.successful(createParam(value))
      else FastFuture.failed(new IllegalArgumentException(errorMessage))
    }
}
