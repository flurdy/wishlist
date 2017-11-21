name := """wishlist"""

version := "7.4-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

resolvers += "flurdy-maven" at "http://dl.bintray.com/content/flurdy/maven"

libraryDependencies ++= {
   val enumeratumVersion = "1.5.12"
   val playMailerVersion = "6.0.1"
   Seq(
      guice,
      jdbc,
      "com.typesafe.play" %% "anorm" % "2.5.3",
      evolutions,
      filters,
      "com.github.t3hnar" %% "scala-bcrypt" % "3.0",
      "com.typesafe.play" %% "play-mailer" % playMailerVersion,
      "com.typesafe.play" %% "play-mailer-guice" % playMailerVersion,
      "org.postgresql" % "postgresql" % "9.4.1212",
      "com.h2database" % "h2" % "1.4.193",
      "com.heroku.sdk" % "heroku-jdbc" % "0.1.1",
      "com.andersen-gott" %% "scravatar" % "1.0.3",
      "com.flurdy" % "sander-core_2.11" % "0.2.0",
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "com.beachape" %% "enumeratum-play" % enumeratumVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
      "com.flurdy" %% "scalasoup" % "0.1.0" % Test,
      "org.mockito" % "mockito-core" % "2.8.9" % Test
   )
}

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)

sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

herokuProcessTypes in Compile := Map(
  "web" -> "target/universal/stage/bin/wishlist -Dconfig.resource=production.conf -Dhttp.port=$PORT"
)

herokuAppName in Compile := sys.props.getOrElse("herokuApp","")
