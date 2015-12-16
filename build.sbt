name := """wishlist"""

version := "7.2-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  "com.typesafe.play" %% "anorm" % "2.4.0",
  evolutions,
  cache,
  ws,
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "com.heroku.sdk" % "heroku-jdbc" % "0.1.1",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.andersen-gott" %% "scravatar" % "1.0.3",
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  specs2 % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
