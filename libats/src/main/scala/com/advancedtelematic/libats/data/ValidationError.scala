package com.advancedtelematic.libats.data

import scala.util.control.NoStackTrace

final case class ValidationError(msg: String) extends Throwable with NoStackTrace