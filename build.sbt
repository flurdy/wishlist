name := """wishlist"""

version := "7.3-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  "com.typesafe.play" %% "anorm" % "2.5.0",
  evolutions,
  cache,
  ws exclude("commons-logging","commons-logging"),
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test,
  "org.jsoup" % "jsoup" % "1.10.2"
)
