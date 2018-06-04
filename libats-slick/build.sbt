name := "libats-slick"

libraryDependencies ++= {
  val slickV = "3.2.1"
  val scalaTestV = "3.0.5"

  Seq(
    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.flywaydb" % "flyway-core" % "4.0.3",

    "org.scalatest"     %% "scalatest" % scalaTestV % Provided,

    "org.mariadb.jdbc" % "mariadb-java-client" % "2.2.5" % Test,

    "org.bouncycastle" % "bcprov-jdk15on" % "1.57" % Provided,
    "org.bouncycastle" % "bcpkix-jdk15on" % "1.57" % Provided
  )
}
