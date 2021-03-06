name := "libats-metrics-finagle"

libraryDependencies ++= {

  val finagleV = "19.9.0"
  val typesafeUriV = "0.1.2"

  Seq(
    "com.twitter" %% "finagle-http" % finagleV,
    "com.advancedtelematic" %% "typesafe-uri" % typesafeUriV
  )
}
