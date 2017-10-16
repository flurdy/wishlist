package notifiers

import com.google.inject.ImplementedBy
import javax.inject.{Inject,Singleton}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer._
import scala.concurrent.Future
// import play.api.{Mode, Play}
// import play.core.Router
import controllers.WithLogging
import models._


@ImplementedBy(classOf[DefaultEmailNotifier])
trait EmailNotifier extends WithLogging {

   def emailDispatcher: EmailDispatcher
   def emailTemplates: EmailTemplates

   def sendContactEmail(name: String, email: String, username: Option[String],
                     subject: Option[String], message: String,
                     currentRecipient: Option[Recipient]): Future[Unit] = {
      val emailMessage = emailTemplates.contactMessageText(
         name, email, username, subject, message, currentRecipient)
      emailDispatcher.sendContactEmail(emailMessage)
   }

   def sendNewRegistrationAlert(recipient: Recipient): Future[Unit] = {
      val emailMessage = emailTemplates.registrationAlertText(recipient.username)
      emailDispatcher.sendAlertEmail(emailMessage)
   }

   def sendRecipientDeletedAlert(recipient: Recipient): Future[Unit] = {
      val emailMessage = emailTemplates.deleteRecipientAlertText(recipient.username)
      emailDispatcher.sendAlertEmail(emailMessage)
   }

   def sendRecipientDeletedNotification(recipient: Recipient): Future[Unit] = {
      val emailMessage = emailTemplates.deleteRecipientNotificationText(recipient.username)
      emailDispatcher.sendNotificationEmail(recipient.email, emailMessage)
   }

   def sendEmailVerification(recipient: Recipient, verificationHash: String): Future[Unit] = {
      val emailMessage = emailTemplates.emailVerificationText(recipient.username, verificationHash)
      logger.info(s"Verification: $verificationHash")
      emailDispatcher.sendNotificationEmail( recipient.email, emailMessage)
   }

   def sendPasswordResetEmail(recipient: Recipient, password: String): Future[Unit] = {
      val emailMessage = emailTemplates.newPasswordText(password)
      emailDispatcher.sendNotificationEmail( recipient.email, emailMessage)
   }

   def sendPasswordChangedNotification(recipient: Recipient): Future[Unit] = {
      val emailMessage = emailTemplates.changePasswordText
      emailDispatcher.sendNotificationEmail( recipient.email, emailMessage)
   }

}

@Singleton
class DefaultEmailNotifier @Inject() (val emailDispatcher: EmailDispatcher, val emailTemplates: EmailTemplates) extends EmailNotifier



@ImplementedBy(classOf[SmtpEmailDispatcher])
trait EmailDispatcher {

   def emailConfig: EmailConfig

   def sendEmail(sender: String, recipient: String, emailMessage: EmailMessage): Future[Unit]

   def sendNotificationEmail(recipient: String, emailMessage: EmailMessage) =
      sendEmail(emailConfig.emailSender, recipient, emailMessage)

   def sendAlertEmail(emailMessage: EmailMessage) =
      sendEmail(emailConfig.emailSender, emailConfig.alertRecipient, emailMessage)

   def sendContactEmail(emailMessage: EmailMessage) =
      sendEmail(emailConfig.emailSender, emailConfig.alertRecipient, emailMessage)

}

@Singleton
class SmtpEmailDispatcher @Inject() (val emailConfig: EmailConfig) extends EmailDispatcher with WithLogging {

   override def sendEmail(sender: String, recipient: String, emailMessage: EmailMessage) = {
      // val mail = Email(
      //            EmailTemplate.subjectPrefix + subjectAndBody._1,
      //            EmailConfiguration.emailFrom,
      //            Seq(recipient),
      //            bodyText = Some(subjectAndBody._2 + EmailTemplate.footer) )
      // mailerClient.send(mail)
      logger.info(s"Email sent: [${emailMessage.subject}] to [$recipient]")
      Future.successful(())
  }

}

/*

object MockEmailDispatcher extends EmailDispatcher {

  override def sendEmail(recipient:String,subjectAndBody:(String,String)) {
    Logger.info("Email sent (mock): [%s] to [%s]" .format(subjectAndBody._1,recipient))
    Logger.info("%s" .format(subjectAndBody._2))
  }

}


trait MailerComponent {

  val mailerClient = new CommonsMailer(Play.configuration)
}




trait EmailService {

  private def noSmtpHostDefinedException = throw new NullPointerException("No SMTP host defined")

  def dispatcher:EmailDispatcher = {
    if (Play.mode == Mode.Prod) {
      Play.configuration.getString("smtp.host") match {
        case None => noSmtpHostDefinedException
        case Some("mock") => MockEmailDispatcher
        case _ => SmtpEmailDispatcher
      }
    } else MockEmailDispatcher
  }

}
*/
