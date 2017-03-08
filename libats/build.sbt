name := "libats"

libraryDependencies ++= {
  val slickV = "3.1.1"
  val metricsV = "3.1.2"
  val scalaTestV = "3.0.0"

  Seq(
    "io.dropwizard.metrics" % "metrics-core" % metricsV,
    "io.dropwizard.metrics" % "metrics-jvm" % metricsV,

    "com.typesafe.slick" %% "slick" % slickV % "provided",
    "com.typesafe.slick" %% "slick-hikaricp" % slickV % "provided",
    "org.flywaydb" % "flyway-core" % "4.0.3" % "provided",

    "org.scalatest"     %% "scalatest" % scalaTestV % "provided"
  )
}
