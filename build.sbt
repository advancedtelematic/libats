val Library = new {
  object Version {
    val akka = "2.6.5"
    val akkaHttp = "10.1.12"
    val akkaHttpCirce = "1.29.1"
    val circe = "0.12.3"
    val refined = "0.9.10"
    val scalaTest = "3.0.8"
    val metricsV = "4.1.0"
    val cats = "2.0.0"
    val logback = "1.2.3"
  }

  val logback = "ch.qos.logback" % "logback-classic" % Version.logback

  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.akka

  val akkaStream = "com.typesafe.akka" %% "akka-stream" % Version.akka

  val Akka = Set(
    "com.typesafe.akka" %% "akka-slf4j",
    "com.typesafe.akka" %% "akka-actor",
    "com.typesafe.akka" %% "akka-stream"
  ).map(_ % Version.akka)

  val akkaHttp = Seq(
      "com.typesafe.akka" %% "akka-http" % Version.akkaHttp,
      "de.heikoseeberger" %% "akka-http-circe" % Version.akkaHttpCirce
    ) ++ Akka

  val akkaHttpTestKit = Seq(
    "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
  ).map(_ % Test)

  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
  ).map(_ % Version.circe)

  val refined = "eu.timepit" %% "refined" % Version.refined

  val scalatest = "org.scalatest" %% "scalatest" % Version.scalaTest % "test,provided"

  val jvmMetrics =  Seq(
    "io.dropwizard.metrics" % "metrics-core" % Version.metricsV,
    "io.dropwizard.metrics" % "metrics-jvm" % Version.metricsV,
    "io.dropwizard.metrics" % "metrics-logback" % Version.metricsV
  )

  val cats = Seq(
    "org.typelevel" %% "cats-core" % Version.cats,
    "org.typelevel" %% "cats-kernel" % Version.cats,
    "org.typelevel" %% "cats-macros" % Version.cats
  )

  val brave = Seq(
    "io.zipkin.brave" % "brave" % "5.6.8",
    "io.zipkin.brave" % "brave-instrumentation-http" % "5.6.8",
    "io.zipkin.reporter2" % "zipkin-sender-okhttp3" % "2.10.3"
  )
}

lazy val commonDeps =
  libraryDependencies ++= Library.circe ++ Seq(Library.refined, Library.scalatest) ++ Library.cats :+ Library.logback

lazy val commonConfigs = Seq.empty

lazy val commonSettings = Seq(
  organization := "com.advancedtelematic",
  licenses += ("MPL-2.0", url("http://mozilla.org/MPL/2.0/")),
  scalaVersion := "2.12.10",
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-Ypartial-unification", "-Xexperimental"),
  resolvers ++= Seq(
    "ATS Releases" at "https://nexus.ota.here.com/content/repositories/releases",
    "ATS Snapshots" at "https://nexus.ota.here.com/content/repositories/snapshots",
    "Central" at "https://nexus.ota.here.com/content/repositories/central",
    "version99 Empty loggers" at "https://version99.qos.ch",
  ),
  buildInfoOptions += BuildInfoOption.ToMap,
  buildInfoOptions += BuildInfoOption.BuildTime) ++ Versioning.settings

lazy val sonarSettings = Seq(
  sonarProperties ++= Map(
    "sonar.projectName" -> "OTA Connect LibATS",
    "sonar.projectKey" -> "ota-connect-libats",
    "sonar.host.url" -> "https://sonar7.devtools.in.here.com",
    "sonar.links.issue" -> "https://saeljira.it.here.com/projects/OTA/issues",
    "sonar.links.scm" -> "https://main.gitlab.in.here.com/olp/edge/ota/connect/back-end/libats-tuf",
    "sonar.links.ci" -> "https://main.gitlab.in.here.com/olp/edge/ota/connect/back-end/libats/pipelines",
    "sonar.language" -> "scala",
    "sonar.scala.coverage.reportPaths"->"libats/target/scala-2.12/scoverage-report/scoverage.xml",
    "sonar.projectVersion" -> version.value,
    "sonar.modules" -> "libats,libats-http,libats-http-tracing,libats-slick,libats-messaging-datatype,libats-messaging,libats-metrics,libats-metrics-kafka,libats-metrics-akka,libats-metrics-prometheus,libats-auth,libats-logging",
    "libats.sonar.projectName" -> "OTA Connect LibATS",
    "libats.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-http.sonar.projectName" -> "OTA Connect LibATS-HTTP",
    "libats-http.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-http.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-http-tracing.sonar.projectName" -> "OTA Connect LibATS-HTTP-Tracing",
    "libats-http-tracing.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-http-tracing.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-slick.sonar.projectName" -> "OTA Connect LibATS-Slick",
    "libats-slick.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-slick.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-messaging-datatype.sonar.projectName" -> "OTA Connect LibATS-Messaging-Datatype",
    "libats-messaging-datatype.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-messaging-datatype.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-messaging.sonar.projectName" -> "OTA Connect LibATS-Messaging",
    "libats-messaging.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-messaging.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-metrics.sonar.projectName" -> "OTA Connect LibATS-Metrics",
    "libats-metrics.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-metrics.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-metrics-kafka.sonar.projectName" -> "OTA Connect LibATS-Metrics-Kafka",
    "libats-metrics-kafka.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-metrics-kafka.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-metrics-akka.sonar.projectName" -> "OTA Connect LibATS-Metrics-Akka",
    "libats-metrics-akka.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-metrics-akka.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-metrics-prometheus.sonar.projectName" -> "OTA Connect LibATS-Metrics-Prometheus",
    "libats-metrics-prometheus.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-metrics-prometheus.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-auth.sonar.projectName" -> "OTA Connect LibATS-Auth",
    "libats-auth.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-auth.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
    "libats-logging.sonar.projectName" -> "OTA Connect LibATS-Logging",
    "libats-logging.sonar.java.binaries" -> "./target/scala-*/classes",
    "libats-logging.sonar.scala.coverage.reportPaths"->"target/scala-2.12/scoverage-report/scoverage.xml",
  )
)

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
  .settings(libraryDependencies ++= Library.jvmMetrics)
  .settings(libraryDependencies ++= Library.circe)
  .settings(libraryDependencies ++= Library.akkaHttpTestKit)
  .settings(Publish.settings)
  .dependsOn(libats)
  .dependsOn(libats_metrics)

lazy val libats_http_tracing = (project in file("libats-http-tracing"))
  .settings(name := "libats-http-tracing")
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonDeps)
  .settings(commonSettings)
  .dependsOn(libats_http)
  .settings(libraryDependencies ++= Library.brave)
  .settings(Publish.settings)
  .dependsOn(libats)

lazy val libats_slick = (project in file("libats-slick"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonDeps)
  .settings(commonSettings)
  .settings(Publish.settings)
  .settings(libraryDependencies ++= Library.jvmMetrics)
  .settings(libraryDependencies ++= Library.akkaHttpTestKit)
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
  .settings(libraryDependencies ++= Library.akkaHttpTestKit)
  .dependsOn(libats)
  .dependsOn(libats_metrics)
  .dependsOn(libats_http)
  .dependsOn(libats_messaging_datatype)

lazy val libats_metrics = (project in file("libats-metrics"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Library.akkaHttp)
  .settings(libraryDependencies ++= Library.circe :+ Library.akkaStream)
  .settings(libraryDependencies ++= Library.jvmMetrics)
  .settings(Publish.settings)

lazy val libats_metrics_kafka = (project in file("libats-metrics-kafka"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(Publish.settings)
  .dependsOn(libats_metrics)
  .dependsOn(libats_metrics_prometheus)
  .dependsOn(libats_messaging)

lazy val libats_metrics_akka = (project in file("libats-metrics-akka"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(Publish.settings)
  .dependsOn(libats_metrics)
  .dependsOn(libats_http)

lazy val libats_metrics_prometheus = (project in file("libats-metrics-prometheus"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(Publish.settings)
  .dependsOn(libats_metrics)
  .dependsOn(libats_http)

lazy val libats_auth = (project in file("libats-auth"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(commonDeps)
  .settings(Publish.settings)
  .settings(libraryDependencies += Library.akkaSlf4j)
  .dependsOn(libats)

lazy val libats_logging = (project in file("libats-logging"))
  .enablePlugins(BuildInfoPlugin, Versioning.Plugin)
  .configs(commonConfigs: _*)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Library.circe :+ Library.logback)
  .settings(name := "libats-logging")
  .settings(Publish.settings)

lazy val libats_root = (project in file("."))
  .enablePlugins(DependencyGraph)
  .settings(Publish.disable)
  .settings(scalaVersion := "2.12.10")
  .aggregate(libats, libats_http, libats_http_tracing, libats_messaging, libats_messaging_datatype,
    libats_slick, libats_auth, libats_metrics, libats_metrics_kafka, libats_metrics_akka,
    libats_metrics_prometheus, libats_logging)
  .settings(sonarSettings)
  .settings(aggregate in sonarScan := false)
    .settings(
      // onLoad is scoped to Global because there's only one.
      onLoad in Global := {
        val old = (onLoad in Global).value
        // compose the new transition on top of the existing one
        // in case your plugins are using this hook.
        startupTransition compose old
    })

lazy val startupTransition: State => State = { s: State =>
  "dependencyUpdates" :: s
}
