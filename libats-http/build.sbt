name := "libats-http"

libraryDependencies ++= {
  val metricsV = "3.1.2"

  Seq(
    "io.dropwizard.metrics" % "metrics-core" % metricsV,
    "io.dropwizard.metrics" % "metrics-jvm" % metricsV,
    "io.dropwizard.metrics" % "metrics-logback" % metricsV
  )
}
