package notifiers

import com.google.inject.ImplementedBy
import javax.inject._
import models._

case class EmailMessage(subject: String, body: String)
case class EmailModel(message: EmailMessage, recipient: String, sender: String)

@ImplementedBy(classOf[DefaultEmailTemplates])
trait EmailTemplates {

   def emailConfig: EmailConfig

   val subjectPrefix = "Wish: "

   lazy val footer =
      s"""
         |
         |
         | Sent by Wish.
         | Wish: create, share and find wish lists online.
         | Visit: http://${emailConfig.hostname}
      """.stripMargin

   def registrationAlertText(username: String) =
      EmailMessage("Alert: New registration",
         s"Recipient $username has registered with Wish")

   def deleteRecipientAlertText(username: String) =
      EmailMessage("Alert: Recipient deleted",
         s"Recipient $username has been deleted from Wish.")

   def deleteRecipientNotificationText(username: String) =
      EmailMessage("Recipient deleted",
         s"""
            | Recipient $username has been deleted from Wish.
            |
            | We are sorry to see you leave.
         """.stripMargin)

   def newPasswordText(newPassword: String) =
      EmailMessage("Password reset",
         s"""
            | Your new password is : $newPassword
            |
            | If you didn't request this password reset for Wish, please let us know at http://${emailConfig.hostname}
         """.stripMargin)


   lazy val changePasswordText =
      EmailMessage("Password changed",
         s"""
            | Your password for Wish has just been changed.
            |
            | If you didn't request this password change for Wish, please let us know at http://${emailConfig.hostname}
         """.stripMargin)

   def emailVerificationText(username: String, verificationUrl: String) =
      EmailMessage("Please verify your email address",
         s"""
            | Hi, welcome to Wish.
            |
            | Please verify your email address by clicking on the link below:
            | $verificationUrl
            |
            |
            | If you didn't register with Wish, please let us know at http://${emailConfig.hostname}
         """.stripMargin)

   def contactMessageText(name: String, email: String, username: Option[String],
                           subject: Option[String], message: String,
                           currentRecipient: Option[Recipient]) = {
      val actualSubject = subject.getOrElse("No subject entered")
      val actualUsername = username.getOrElse("No username entered")
      val actualRecipient = currentRecipient.map( _.username )
                                            .getOrElse("No current recipient")
      EmailMessage("Contact message",
         s"""
            | Current recipient: $actualRecipient
            | Name: $name
            | Email: $email
            | Username: $actualUsername
            |
            | Subject: $actualSubject
            |
            | Message:
            | $message
         """.stripMargin)
   }
}

@Singleton
class DefaultEmailTemplates @Inject() (val emailConfig: EmailConfig) extends EmailTemplates
