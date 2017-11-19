package notifiers

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.anyString
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.mailer._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import controllers.BaseUnitSpec
import models.{EmailConfig,Recipient}

class EmailDispatcherSpec extends BaseUnitSpec with ScalaFutures {

   trait Setup {

      val emailConfigMock = mock[EmailConfig]
      val emailTemplatesMock = mock[EmailTemplates]
      val mailerClientMock = mock[MailerClient]

      val dispatcher = new SmtpEmailDispatcher(emailConfigMock, emailTemplatesMock, mailerClientMock)
      val subject = "some subject"
      val subjectPrefix = "some prefix"
      val body = "some body"
      val footer = "some footer"
      val recipient = "some-recipient@example.com"
      val alertRecipient = "some-alert-recipient@example.com"
      val sender = "some-sender@example.com"
      val message = EmailMessage( subject, body)
      val mail = Email(
            subject = s"$subjectPrefix $subject",
            from = sender,
            to = Seq(recipient),
            bodyText = Some(s"$body $footer"))
      val alertMail = mail.copy(to = Seq(alertRecipient))

      when(emailConfigMock.emailSender).thenReturn(sender)
      when(emailConfigMock.alertRecipient).thenReturn(alertRecipient)
      when(emailTemplatesMock.subjectPrefix).thenReturn(subjectPrefix)
      when(emailTemplatesMock.footer).thenReturn(footer)
   }

   "EmailDispatcher" should {
      "send email" when calling {
         "sendNotificationEmail" in new Setup {

            whenReady( dispatcher.sendNotificationEmail( recipient, message) ){ _ =>

               verify( mailerClientMock ).send(mail)
            }
         }
         "sendAlertEmail" in new Setup {
            whenReady( dispatcher.sendAlertEmail( message ) ){ _ =>

               verify( mailerClientMock ).send(alertMail)
            }
         }
         "sendContactEmail" in new Setup {
            whenReady( dispatcher.sendContactEmail( message ) ){ _ =>

               verify( mailerClientMock ).send(alertMail)
            }
         }
      }
   }

}
