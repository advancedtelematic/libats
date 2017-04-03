name := "libats-metrics"

libraryDependencies ++=
  Seq("metrics-core", "metrics-jvm").map("io.dropwizard.metrics" % _ % "3.2.2")
