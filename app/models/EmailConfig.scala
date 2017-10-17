package models

import com.google.inject.ImplementedBy
import javax.inject._


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
