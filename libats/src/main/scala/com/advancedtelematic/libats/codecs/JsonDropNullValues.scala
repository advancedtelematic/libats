package com.advancedtelematic.libats.codecs

import io.circe.Json

object JsonDropNullValues {
  implicit class JsonDropNullValuesOps(value: Json) {
    private def rec(counter: Int)(json: Json): Json = {
      if (counter == 0)
        json
      else
        json.arrayOrObject[Json](
          json,
          array => Json.fromValues(array.map(rec(counter - 1))),
          obj => Json.fromJsonObject(obj.filter { case (_, v) => !v.isNull }.mapValues(rec(counter - 1)))
        )
    }

    // Drops values recursively, limited to 5 levels
    def dropNullValuesDeep: Json = rec(5)(value)
  }
}
