package com.advancedtelematic.libats.boot

trait VersionInfoProvider {
  val name: String
  val version: String
  val builtAtMillis: Long
  val toMap: Map[String, Any]
}

trait VersionInfo {
  protected val provider: VersionInfoProvider

  lazy val projectName: String = provider.name
  lazy val version: String = provider.version
  lazy val nameVersion: String = provider.name + "/" + provider.version
  lazy val builtAtMillis: Long = provider.builtAtMillis
  lazy val versionMap: Map[String, Any] = provider.toMap
}
