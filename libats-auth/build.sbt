name := "libats-auth"

libraryDependencies ++= {
  val JsonWebSecurityV = "0.4.5-7-g0239acb"

  Seq(
    "com.advancedtelematic" %% "jw-security-core" % JsonWebSecurityV,
    "com.advancedtelematic" %% "jw-security-jca" % JsonWebSecurityV,
    "com.advancedtelematic" %% "jw-security-akka-http" % JsonWebSecurityV
  )
}
