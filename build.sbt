name := """wishlist"""

version := "7.4-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.21"

libraryDependencies ++= {
  val enumeratumVersion = "1.5.15"
  val playMailerVersion = "6.0.1"
  Seq(
    guice,
    jdbc,
    "org.playframework.anorm" %% "anorm" % "2.6.10",
    evolutions,
    filters,
    "com.github.t3hnar" %% "scala-bcrypt" % "3.1",
    "com.typesafe.play" %% "play-mailer" % playMailerVersion,
    "com.typesafe.play" %% "play-mailer-guice" % playMailerVersion,
    "org.postgresql" % "postgresql" % "42.7.14",
    "com.h2database" % "h2" % "1.4.200",
    "com.heroku.sdk" % "heroku-jdbc" % "0.1.1",
    "com.andersen-gott" %% "scravatar" % "1.0.3",
    "com.beachape" %% "enumeratum" % enumeratumVersion,
    "com.beachape" %% "enumeratum-play" % enumeratumVersion,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % Test,
    "org.mockito" % "mockito-core" % "2.28.2" % Test
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

herokuAppName in Compile := sys.props.getOrElse("herokuApp", "")
