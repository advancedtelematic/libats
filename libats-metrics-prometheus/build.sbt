name := "libats-metrics-prometheus"

libraryDependencies ++= {
  Seq(
    "io.prometheus" % "simpleclient_common" % "0.4.0",
    "io.prometheus" % "simpleclient_dropwizard" % "0.4.0"
  )
}
