package com.advancedtelematic.libats.messaging_datatype

object DataType {
  case class PackageId(name: String, version: String) {
    def mkString = s"$name-$version"
  }
}
