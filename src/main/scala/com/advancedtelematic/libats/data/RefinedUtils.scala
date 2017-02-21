/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package com.advancedtelematic.libats.data

import com.advancedtelematic.libats.codecs.RefinementError
import eu.timepit.refined
import eu.timepit.refined.api.{Refined, Validate}

import scala.util.{Failure, Success, Try}

object RefinedUtils {
  implicit class RefineTry[T](value: T) {
    def refineTry[P](implicit ev: Validate[T, P]): Try[Refined[T, P]] = {
      refined.refineV(value) match {
        case Left(err) => Failure(RefinementError(value, err))
        case Right(v) => Success(v)
      }
    }
  }
}
