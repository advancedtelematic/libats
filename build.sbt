name := "libats"
organization := "com.advancedtelematic.com"
scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "ATS Releases" at "http://nexus.prod01.internal.advancedtelematic.com:8081/content/repositories/releases"

resolvers += "ATS Snapshots" at "http://nexus.prod01.internal.advancedtelematic.com:8081/content/repositories/snapshots"

lazy val libats = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoOptions += BuildInfoOption.ToMap,
    buildInfoOptions += BuildInfoOption.BuildTime
  )
  .enablePlugins(Versioning.Plugin)
  .settings(Versioning.settings)
  .settings(Publish.settings)
  .settings(Seq(libraryDependencies ++= {
    val akkaV = "2.4.17"
    val akkaHttpV = "10.0.3"
    val slickV = "3.1.1"
    val circeV = "0.4.1"
    val metricsV = "3.1.2"
    val scalaTestV = "3.0.0"

    Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaV,
      "com.typesafe.akka" %% "akka-stream" % akkaV,
      "com.typesafe.akka" %% "akka-http" % akkaHttpV,
      "com.typesafe.akka" %% "akka-slf4j" % akkaV,

      "de.heikoseeberger" %% "akka-http-circe" % "1.7.0",

      "eu.timepit" %% "refined" % "0.3.1",

      "io.dropwizard.metrics" % "metrics-core" % metricsV,
      "io.dropwizard.metrics" % "metrics-jvm" % metricsV,

      "io.circe" %% "circe-core" % circeV,
      "io.circe" %% "circe-generic" % circeV,
      "io.circe" %% "circe-parser" % circeV,
      "io.circe" %% "circe-java8" % circeV,

      "com.typesafe.slick" %% "slick" % slickV,
      "com.typesafe.slick" %% "slick-hikaricp" % slickV,
      "org.flywaydb" % "flyway-core" % "4.0.3",

      "org.scalatest"     %% "scalatest" % scalaTestV % "provided"
    )
  }))
