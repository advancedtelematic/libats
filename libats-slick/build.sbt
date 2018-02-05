name := "libats-slick"

libraryDependencies ++= {
  val slickV = "3.2.1"
  val scalaTestV = "3.0.5"

  Seq(
    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.flywaydb" % "flyway-core" % "4.0.3",

    "org.scalatest"     %% "scalatest" % scalaTestV % "provided",

    "org.mariadb.jdbc" % "mariadb-java-client" % "1.4.4" % Test
  )
}
