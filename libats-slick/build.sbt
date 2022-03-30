name := "libats-slick"

libraryDependencies ++= {
  val slickV = "3.2.3"
  val scalaTestV = "3.0.8"

  Seq(
    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.flywaydb" % "flyway-core" % "6.0.8",

    "org.scalatest"     %% "scalatest" % scalaTestV % Provided,
    "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,

    "org.mariadb.jdbc" % "mariadb-java-client" % "2.4.4" % Test,

    "org.bouncycastle" % "bcprov-jdk15on" % "1.63" % Provided,
    "org.bouncycastle" % "bcpkix-jdk15on" % "1.63" % Provided
  )
}
