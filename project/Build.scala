import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "wishlist"
    val appVersion      = "7.0-SNAPSHOT"

    val appDependencies = Seq(
      "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
      "org.mindrot" % "jbcrypt" % "0.3m",
      "com.typesafe" %% "play-plugins-mailer" % "2.0.4",
      "com.andersen-gott" %% "scravatar" % "1.0.1"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
           
    )

}
