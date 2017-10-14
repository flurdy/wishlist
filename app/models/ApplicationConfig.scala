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

@ImplementedBy(classOf[DefaultEmailConfig])
trait EmailConfig {

   protected def appConfig: ApplicationConfig

   lazy val hostname  = appConfig.getString("email.hostname")
                                 .getOrElse(throw new IllegalStateException("Missing configuration"))
   lazy val emailSender = appConfig.getString("email.sender.mail")
                                   .getOrElse(throw new IllegalStateException("Missing configuration"))
   lazy val alertRecipient = appConfig.getString("email.alerts.recipient.mail")
                                      .getOrElse(throw new IllegalStateException("Missing configuration"))

}

@Singleton
class DefaultEmailConfig @Inject() (val appConfig: ApplicationConfig) extends EmailConfig
