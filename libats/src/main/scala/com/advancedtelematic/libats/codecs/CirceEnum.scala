/*
 * Copyright (C) 2016 HERE Global B.V.
 *
 * Licensed under the Mozilla Public License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.mozilla.org/en-US/MPL/2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: MPL-2.0
 * License-Filename: LICENSE
 */

package com.advancedtelematic.libats.codecs

import io.circe.{Decoder, Encoder}

@deprecated("Consider using a sealed trait, enumeratum or define custom codecs using Encoder.enumEncoder and Decoder.enumDecoder", "v0.0.1-104")
trait CirceEnum extends Enumeration {
  implicit val encode: Encoder[Value] = Encoder.enumEncoder(this)
  implicit val decode: Decoder[Value] = Decoder.enumDecoder(this)
}
