import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "wishlist"
    val appVersion      = "7.1-SNAPSHOT"

    val appDependencies = Seq(
      jdbc,
      anorm,
      "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
      "org.mindrot" % "jbcrypt" % "0.3m",
      "com.typesafe" %% "play-plugins-mailer" % "2.2.0",
      "com.andersen-gott" %% "scravatar" % "1.0.2",
      "net.databinder.dispatch" %% "dispatch-core" % "0.9.2"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(

    )

}
