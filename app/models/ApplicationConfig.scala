package models

import com.google.inject.ImplementedBy
import javax.inject._
import play.api.Configuration


@ImplementedBy(classOf[DefaultApplicationConfig])
trait ApplicationConfig {

   protected def config: Configuration

   def findConfig(property: String): Option[Configuration]  = config.getOptional[Configuration](property)
   def findInt(property: String): Option[Int]         = config.getOptional[Int](property)
   def findBoolean(property: String): Option[Boolean] = config.getOptional[Boolean](property)
   def findString(property: String): Option[String]   = config.getOptional[String](property)

}

@Singleton
class DefaultApplicationConfig @Inject() (configuration: Configuration) extends ApplicationConfig {

   override lazy val config =
       configuration.getOptional[Configuration]("com.flurdy.wishlist")
                    .getOrElse(Configuration.empty)

}

trait WithApplicationConfig {
   protected def appConfig: ApplicationConfig
}
