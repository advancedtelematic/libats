name := "libats-metrics-finagle"

libraryDependencies ++= {

  val finagleV = "6.44.0"
  val typesafeUriV = "0.1.1"

  Seq(
    "com.twitter" %% "finagle-http" % finagleV,
    "com.advancedtelematic" %% "typesafe-uri" % typesafeUriV
  )
}