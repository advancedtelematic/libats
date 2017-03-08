name := "libats-messaging"

libraryDependencies ++= {
  Seq(
    "com.github.tyagihas" %% "scala_nats" % "0.2.1" exclude("org.slf4j", "slf4j-simple"),
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.12"
  )
}
