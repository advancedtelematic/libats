val Library = new {
  object Version {
    val akka = "2.4.17"
    val akkaHttp = "10.0.5"
    val akkaHttpCirce = "1.15.0"
    val circe = "0.7.1"
    val refined = "0.8.0"
    val scalaTest = "3.0.0"
  }

  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.akka

  val akkaStream = "com.typesafe.akka" %% "akka-stream" % Version.akka

  val akkaHttp = Seq(
      "com.typesafe.akka" %% "akka-http" % Version.akkaHttp,
      "de.heikoseeberger" %% "akka-http-circe" % Version.akkaHttpCirce
    )

  val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp % "test"

  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-java8"
  ).map(_ % Version.circe)

  val refined = "eu.timepit" %% "refined" % Version.refined

  val scalatest = "org.scalatest" %% "scalatest" % Version.scalaTest % "test,provided"
}

lazy val commonDeps =
  libraryDependencies ++= Library.circe ++ Seq(Library.refined, Library.scalatest)

lazy val commonConfigs = Seq.empty

lazy val commonSettings = Seq(
  organization := "com.advancedtelematic",
  scalaVersion := "2.11.11",
  crossScalaVersions := Seq("2.11.11", "2.12.2"),
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature"),
  resolvers += "ATS Releases" at "http://nexus.advancedtelematic.com:8081/content/repositories/releases",
  resolvers += "ATS Snapshots" at "http://nexus.advancedtelematic.com:8081/content/repositories/snapshots",
  resolvers += "version99 Empty loggers" at "http://version99.qos.ch",
  buildInfoOptions += BuildInfoOption.ToMap,
  buildInfoOptions += BuildInfoOption.BuildTime) ++ Versioning.settings

lazy val libats = (project in file("libats"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonDeps)
  .settings(commonSettings)
  .settings(Publish.settings)

lazy val libats_http = (project in file("libats-http"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonDeps)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Library.akkaHttp)
  .settings(Publish.settings)
  .dependsOn(libats)

lazy val libats_slick = (project in file("libats-slick"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonDeps)
  .settings(commonSettings)
  .settings(Publish.settings)
  .dependsOn(libats)
  .dependsOn(libats_http)

lazy val libats_messaging_datatype = (project in file("libats-messaging-datatype"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(commonDeps)
  .settings(Publish.settings)
  .dependsOn(libats)

lazy val libats_messaging = (project in file("libats-messaging"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonDeps)
  .settings(commonSettings)
  .settings(Publish.settings)
  .settings(libraryDependencies += Library.akkaHttpTestKit)
  .dependsOn(libats)
  .dependsOn(libats_http)
  .dependsOn(libats_messaging_datatype)

lazy val libats_metrics = (project in file("libats-metrics"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Library.akkaHttp)
  .settings(libraryDependencies ++= Library.circe :+ Library.akkaStream)
  .settings(Publish.settings)

lazy val libats_metrics_kafka = (project in file("libats-metrics-kafka"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(Publish.settings)
  .dependsOn(libats_metrics)

lazy val libats_metrics_akka = (project in file("libats-metrics-akka"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(Publish.settings)
  .dependsOn(libats_metrics)
  .dependsOn(libats_http)

lazy val libats_metrics_finagle = (project in file("libats-metrics-finagle"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Library.circe :+ Library.akkaStream)
  .settings(Publish.settings).dependsOn(libats_metrics)

lazy val libats_auth = (project in file("libats-auth"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(commonDeps)
  .settings(Publish.settings)
  .settings(libraryDependencies += Library.akkaSlf4j)
  .dependsOn(libats)

lazy val libats_root = (project in file("."))
  .settings(Publish.disable)
  .settings(scalaVersion := "2.11.11")
  .settings(crossScalaVersions := Seq("2.11.11", "2.12.2"))
  .aggregate(libats, libats_http, libats_messaging, libats_messaging_datatype,
    libats_slick, libats_auth, libats_metrics, libats_metrics_kafka, libats_metrics_akka,
    libats_metrics_finagle)
