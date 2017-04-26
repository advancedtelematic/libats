name := "libats-slick"

libraryDependencies ++= {
  val slickV = "3.2.0"
  val metricsV = "3.1.2"
  val scalaTestV = "3.0.0"

  Seq(
    "io.dropwizard.metrics" % "metrics-core" % metricsV,
    "io.dropwizard.metrics" % "metrics-jvm" % metricsV,

    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.flywaydb" % "flyway-core" % "4.0.3",

    "org.scalatest"     %% "scalatest" % scalaTestV % "provided"
  )
}
