package notifiers

import com.google.inject.ImplementedBy
import javax.inject.{Inject,Singleton}
// import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer._
import scala.concurrent.{ExecutionContext, Future}
import controllers.WithLogging
import models._


@ImplementedBy(classOf[SmtpEmailDispatcher])
trait EmailDispatcher {

   def emailConfig: EmailConfig
   def emailTemplates: EmailTemplates
   def mailerClient: MailerClient

   protected def sendEmail(sender: String, recipient: String, subject: String, body: String): Future[Unit]

   def sendNotificationEmail(recipient: String, emailMessage: EmailMessage) =
      sendMessageEmail( recipient, emailMessage)

   def sendAlertEmail(emailMessage: EmailMessage) =
      sendMessageEmail( emailConfig.alertRecipient, emailMessage)

   def sendContactEmail(emailMessage: EmailMessage) =
      sendMessageEmail( emailConfig.alertRecipient, emailMessage)

   private def sendMessageEmail(recipient: String, emailMessage: EmailMessage) = {
      sendEmail(
         emailConfig.emailSender, recipient,
          s"${emailTemplates.subjectPrefix} ${emailMessage.subject}",
          s"${emailMessage.body} ${emailTemplates.footer}" )
   }
}

@Singleton
class SmtpEmailDispatcher @Inject() (val emailConfig: EmailConfig, val emailTemplates: EmailTemplates, val mailerClient: MailerClient)(implicit executionContext: ExecutionContext) extends EmailDispatcher with WithLogging {

   override def sendEmail(sender: String, recipient: String, subject: String, body: String) = {

      val mail = Email(
         subject = subject,
         from = sender,
         to = Seq(recipient),
         bodyText = Some(body))
      Future {
         mailerClient.send(mail)
         logger.info(s"Email sent: [$subject] to [$recipient]")
      }
   }
}
