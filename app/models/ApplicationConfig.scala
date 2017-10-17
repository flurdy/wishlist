package models

import com.google.inject.ImplementedBy
import javax.inject._
import play.api.Configuration


@ImplementedBy(classOf[DefaultApplicationConfig])
trait ApplicationConfig {

   protected def config: Configuration

   def getConfig(property: String)  = config.getConfig(property)
   def getInt(property: String)     = config.getInt(property)
   def getBoolean(property: String) = config.getBoolean(property)
   def getString(property: String)  = config.getString(property)

}

@Singleton
class DefaultApplicationConfig @Inject() (configuration: Configuration) extends ApplicationConfig {
   override lazy val config =
       configuration.getConfig("com.flurdy.wishlist")
                    .getOrElse(Configuration.empty)
}

trait WithApplicationConfig {
   protected def appConfig: ApplicationConfig
}
