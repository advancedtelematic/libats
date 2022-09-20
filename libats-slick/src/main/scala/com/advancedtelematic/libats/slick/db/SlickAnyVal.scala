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

package com.advancedtelematic.libats.slick.db

import java.util.UUID

import shapeless.{::, Generic, HNil}
import slick.jdbc.MySQLProfile.api._

import scala.reflect.ClassTag

object SlickAnyVal {
  private def dbSerializableAnyValMapping[T <: AnyVal, U: BaseColumnType]
  (implicit gen: Generic.Aux[T, U :: HNil], classTag: ClassTag[T]): BaseColumnType[T] =
    MappedColumnType.base[T, U](
      (v: T) => gen.to(v).head,
      (s: U) => gen.from(s :: HNil)
    )

  // Scala implicit resolution is not smart enough to just use the method above as implicit and not use
  // the `val`s below

  implicit def stringAnyValSerializer[T <: AnyVal]
  (implicit gen: Generic.Aux[T, String :: HNil], classTag: ClassTag[T]): BaseColumnType[T] =
    dbSerializableAnyValMapping[T, String]

  implicit def uuidAnyValSerializer[T <: AnyVal]
  (implicit gen: Generic.Aux[T, UUID :: HNil], classTag: ClassTag[T]): BaseColumnType[T] =
    dbSerializableAnyValMapping[T, UUID](SlickExtensions.uuidColumnType, gen, classTag)
}
