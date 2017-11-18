package models

import com.google.inject.ImplementedBy
import javax.inject._


@ImplementedBy(classOf[DefaultEmailConfig])
trait EmailConfig {

   protected def appConfig: ApplicationConfig

   lazy val hostname  = appConfig.findString("email.hostname")
                                 .getOrElse(throw new IllegalStateException("Missing configuration"))
   lazy val emailSender = appConfig.findString("email.sender.mail")
                                   .getOrElse(throw new IllegalStateException("Missing configuration"))
   lazy val alertRecipient = appConfig.findString("email.alerts.recipient.mail")
                                      .getOrElse(throw new IllegalStateException("Missing configuration"))

}

@Singleton
class DefaultEmailConfig @Inject() (val appConfig: ApplicationConfig) extends EmailConfig
