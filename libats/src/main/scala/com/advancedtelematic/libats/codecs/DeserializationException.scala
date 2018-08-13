/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.codecs

import scala.util.control.NoStackTrace

/**
 * Unmarshalling into JSON sometimes fails, see CirceMarshallingSupport.scala.
 */

case class DeserializationException(cause: Throwable) extends Throwable(cause) with NoStackTrace
