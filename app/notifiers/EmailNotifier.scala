package notifiers

import com.typesafe.plugin._
import models._
import play.api.Play.current
import play.Logger
import play.api.{Mode, Play}
import play.core.Router


object EmailNotifier {

  private def noSmtpHostDefinedException = throw new NullPointerException("No SMTP host defined")
  private val alertRecipient = Play.configuration.getString("mail.alerts").getOrElse("wish@example.org")
  private val emailFrom = Play.configuration.getString("mail.from").getOrElse("wish@example.com")

  private def hostname = Play.configuration.getString("net.hostname").getOrElse("localhost")

  private def footer = {
    """


      Sent by Wish.
      Host: %s
    """.format(hostname)
  }

  private def sendOrMockAlert(notification: (String, String)) {
    if (Play.mode == Mode.Prod) {
      Play.configuration.getString("smtp.host") match {
        case None => noSmtpHostDefinedException
        case Some("mock") => mockNotification(notification._1)
        case _ => sendNotification(alertRecipient,notification._1, notification._2)
      }
    } else {
      mockNotification(notification._1)
    }
  }

  private def sendOrMockNotification(email:String,notification: (String, String)) {
    if (Play.mode == Mode.Prod) {
      Play.configuration.getString("smtp.host") match {
        case None => noSmtpHostDefinedException
        case Some("mock") => mockNotification(notification._1)
        case _ => sendNotification(email,notification._1, notification._2)
      }
    } else {
      mockNotification(notification._1)
    }
  }


  private def sendNotification(recipient:String,subject: String, bodyText: String) {
    val mail = use[MailerPlugin].email
    mail.setSubject("WISH: " + subject)
    mail.addFrom(emailFrom)
    mail.addRecipient(recipient)
    mail.send(bodyText+footer)
    Logger.info("Notification sent: " + subject)
  }


  private def mockNotification(subject: String) {
    Logger.info("Notification (mock): " + subject)
  }


  def registrationAlert(recipient: Recipient) {
    sendOrMockAlert(registrationText(recipient))
  }

  def deleteRecipientAlert(recipient: Recipient) {
    sendOrMockAlert(deleteRecipientAlertText(recipient))
  }

  def deleteRecipientNotification(recipient: Recipient) {
    sendOrMockNotification(recipient.email,deleteRecipientNotificationText(recipient))
  }


  private def registrationText(recipient: Recipient) = {
    ("New registration", "Recipient " + recipient.username + " has registered with Wis")
  }

  private def deleteRecipientAlertText(recipient: Recipient) = {
    ("Recipient deleted",
      """

        Recipient %s has been deleted from Wish.


      """.format(recipient.username))
      }

  private def deleteRecipientNotificationText(recipient: Recipient) = {
    ("Recipient deleted",
      """
         Recipient %s has been deleted from Wish.

         We are sorry to see you leave.

      """.format(recipient.username) )
  }



  def newPasswordText(recipient: Recipient, newPassword: String): (String, String) = {
    ("Password reset","Your new password is : " + newPassword)
  }

  def sendNewPassword(recipient: Recipient, newPassword: String) {
    sendOrMockNotification(recipient.email,newPasswordText(recipient, newPassword))
  }



  def emailVerificationText(username: String, verificationUrl: String): (String, String) = {
    ("Please verify your email address",
      """
        Hi, welcome to Wish.

        Please verify your email address by going to this website:
        %s


        If you didn't register with Snaps, please let us know at %s
      """.format(verificationUrl,hostname))
  }


  def sendEmailVerification(recipient:Recipient, verificationHash: String) {
    val verificationUrl = hostname + "/recipient/" + recipient.recipientId + "/verify/" + verificationHash +"/"
    sendOrMockNotification(recipient.email,emailVerificationText(recipient.username, verificationUrl))
  }
}
