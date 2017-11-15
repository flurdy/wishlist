name := """wishlist"""

version := "7.4-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

resolvers += "flurdy-maven" at "http://dl.bintray.com/content/flurdy/maven"

libraryDependencies ++= {
   val enumeratumVersion = "1.5.12"
   val playMailerVersion = "5.0.0"
   Seq(
      guice,
      jdbc,
      "com.typesafe.play" %% "anorm" % "2.5.1",
      evolutions,
      // cache,
      filters,
      // ws exclude("commons-logging","commons-logging"),
      "com.github.t3hnar" %% "scala-bcrypt" % "3.0",
      "com.typesafe.play" %% "play-mailer" % playMailerVersion,
      // "commons-logging" % "commons-logging" % "1.1.3",
      "org.postgresql" % "postgresql" % "9.4.1212",
      "com.h2database" % "h2" % "1.4.193",
      "com.heroku.sdk" % "heroku-jdbc" % "0.1.1",
      "com.andersen-gott" %% "scravatar" % "1.0.3",
      "com.flurdy" %% "sander-core" % "0.2.0",
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "com.beachape" %% "enumeratum-play" % enumeratumVersion,
      // "com.beachape" %% "enumeratum-play-json" % enumeratumVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test,
      "org.jsoup" % "jsoup" % "1.10.2" % Test,
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
