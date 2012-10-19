package notifiers

import com.typesafe.plugin._
import models._
import play.api.Play.current
import play.Logger
import play.api.{Mode, Play}
import play.core.Router



trait EmailNotifierTrait {

  private def noSmtpHostDefinedException = throw new NullPointerException("No SMTP host defined")

  protected def dispatcher:EmailDispatcher = {
    if (Play.mode == Mode.Prod) {
      Play.configuration.getString("smtp.host") match {
        case None => noSmtpHostDefinedException
        case Some("mock") => MockEmailDispatcher
        case _ => SmtpEmailDispatcher
      }
    } else {
      MockEmailDispatcher
    }
  }
}

object EmailTemplate {
  
}

object EmailConfiguration {

  private val hostname = Play.configuration.getString("net.hostname").getOrElse("localhost")
  private val emailFrom = Play.configuration.getString("mail.from").getOrElse("wish@example.com")

  private def footer = {
    """


      Sent by Wish.
      Host: %s
    """.format(hostname)
  }

}

object EmailNotifier with EmailNotifier {

  def sendPasswordResetEmail {

  }

  def sendRecipientDeletedNotification {

  }

  def sendEmailVerificationEmail(recipient:Recipient, verificationHash: String) {
    val verificationUrl = hostname + "/recipient/" + recipient.recipientId + "/verify/" + verificationHash +"/"
    dispatcher.sendEmail(recipient.email,emailVerificationText(recipient.username, verificationUrl))
  }

} 


object EmailAlerter  with EmailTrait{
  private val alertRecipient = Play.configuration.getString("mail.alerts").getOrElse("wish@example.org")

  def sendNewRegistrationAlert {

  }
  
  def sendRecipientDeletedAlert {

  }

} 

trait EmailDispatcher {

}

object MockEmailDispatcher with EmailDispatcher {


  def sendEmail(subject: String) {
    Logger.info("Notification (mock): " + subject)
  }

}



object SmtpEmailDispatcher with EmailDispatcher {

  def sendEmail(recipient:String,subject: String, bodyText: String) {
    val mail = use[MailerPlugin].email
    mail.setSubject("WISH: " + subject)
    mail.addFrom(emailFrom)
    mail.addRecipient(recipient)
    mail.send(bodyText+footer)
    Logger.info("Notification sent: " + subject)
  }


}

-----


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




  def sendRegistrationAlert(recipient: Recipient) {
    sendOrMockAlert(registrationText(recipient))
  }

  def sendDeleteRecipientAlert(recipient: Recipient) {
    sendOrMockAlert(deleteRecipientAlertText(recipient))
  }

  def sendDeleteRecipientNotification(recipient: Recipient) {
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



  private def newPasswordText(recipient: Recipient, newPassword: String): (String, String) = {
    ("Password reset","Your new password is : " + newPassword)
  }

  def sendNewPassword(recipient: Recipient, newPassword: String) {
    sendOrMockNotification(recipient.email,newPasswordText(recipient, newPassword))
  }



  private def emailVerificationText(username: String, verificationUrl: String): (String, String) = {
    ("Please verify your email address",
      """
        Hi, welcome to Wish.

        Please verify your email address by going to this website:
        %s


        If you didn't register with Snaps, please let us know at %s
      """.format(verificationUrl,hostname))
  }
}
