name := """wishlist"""

version := "7.3-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= {
   val enumeratumVersion = "1.5.8"
   // val playMailerVersion = "6.0.1"
   val playMailerVersion = "5.0.0"
   Seq(
      jdbc,
      "com.typesafe.play" %% "anorm" % "2.5.0",
      evolutions,
      cache,
      filters,
      ws exclude("commons-logging","commons-logging"),
      "com.github.t3hnar" %% "scala-bcrypt" % "3.0",
      "com.typesafe.play" %% "play-mailer" % playMailerVersion,
      // "commons-logging" % "commons-logging" % "1.1.3",
      // "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
      // "com.heroku.sdk" % "heroku-jdbc" % "0.1.1",
      "com.andersen-gott" %% "scravatar" % "1.0.3",
      "com.flurdy"        %% "sander-core" % "0.2.0",
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "com.beachape" %% "enumeratum-play" % enumeratumVersion,
      "com.beachape" %% "enumeratum-play-json" % enumeratumVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test,
      "org.jsoup" % "jsoup" % "1.10.2" % Test,
      "org.mockito" % "mockito-core" % "2.8.9" % Test
   )
}
