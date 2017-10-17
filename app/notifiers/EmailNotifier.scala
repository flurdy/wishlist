package notifiers

import com.google.inject.ImplementedBy
import javax.inject.{Inject,Singleton}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
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


/*

object MockEmailDispatcher extends EmailDispatcher {

  override def sendEmail(recipient:String,subjectAndBody:(String,String)) {
    Logger.info("Email sent (mock): [%s] to [%s]" .format(subjectAndBody._1,recipient))
    Logger.info("%s" .format(subjectAndBody._2))
  }

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
