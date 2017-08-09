name := "libats"

libraryDependencies ++= {
  val slickV = "3.2.0"
  val metricsV = "3.1.2"
  val scalaTestV = "3.0.0"

  Seq(
    "io.dropwizard.metrics" % "metrics-core" % metricsV,
    "io.dropwizard.metrics" % "metrics-jvm" % metricsV,
    "io.dropwizard.metrics" % "metrics-logback" % metricsV,

    "org.scalatest"     %% "scalatest" % scalaTestV % "provided"
  )
}
