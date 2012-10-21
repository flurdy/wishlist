package notifiers

import com.typesafe.plugin._
import models._
import play.api.Play.current
import play.Logger
import play.api.{Mode, Play}
import play.core.Router


object EmailConfiguration {

  def hostname = Play.configuration.getString("net.hostname").getOrElse("localhost")
  def emailFrom = Play.configuration.getString("mail.from").getOrElse("wish@example.com")
  def alertRecipient = Play.configuration.getString("mail.alerts").getOrElse("wish@example.org")

}

trait EmailDispatcher {  

  def sendEmail(recipient:String,subjectAndBody:(String,String))

  def sendAlertEmail(subjectAndBody:(String,String)) {
    sendEmail(EmailConfiguration.alertRecipient,subjectAndBody)
  }

}

object MockEmailDispatcher extends EmailDispatcher {
  
  override def sendEmail(recipient:String,subjectAndBody:(String,String)) {
    Logger.info("Email sent (mock): [%s] to [%s]" .format(subjectAndBody._1,recipient))
  }

}


object SmtpEmailDispatcher extends EmailDispatcher {

  override def sendEmail(recipient:String, subjectAndBody:(String,String)) {
    val mail = use[MailerPlugin].email
    mail.setSubject(EmailTemplate.subjectPrefix + subjectAndBody._1)
    mail.addFrom(EmailConfiguration.emailFrom)
    mail.addRecipient(recipient)
    mail.send(subjectAndBody._2 + EmailTemplate.footer)
    Logger.info("Email sent: [%s] to [%s]" .format(subjectAndBody._1,recipient))
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
    } else {
      MockEmailDispatcher
    }
  }

}



object EmailNotifier extends EmailService {

  def sendPasswordResetEmail(recipient: Recipient, newPassword: String) {
    dispatcher.sendEmail(recipient.email,EmailTemplate.newPasswordText(recipient, newPassword))
  }

  def sendPasswordChangeEmail(recipient: Recipient) {
    dispatcher.sendEmail(recipient.email,EmailTemplate.changePasswordText(recipient))
  }


  def sendRecipientDeletedNotification(recipient: Recipient) {
    dispatcher.sendEmail(recipient.email,EmailTemplate.deleteRecipientNotificationText(recipient))
  }

  def sendEmailVerificationEmail(recipient:Recipient, verificationHash: String) {
    val verificationUrl = EmailConfiguration.hostname + "/recipient/" + recipient.recipientId + "/verify/" + verificationHash +"/"
    dispatcher.sendEmail(recipient.email, EmailTemplate.emailVerificationText(recipient.username, verificationUrl))
  }

} 




object EmailAlerter extends EmailService {

  def sendNewRegistrationAlert(recipient: Recipient) {
    dispatcher.sendAlertEmail(EmailTemplate.registrationText(recipient))  
  }
  
  def sendRecipientDeletedAlert(recipient: Recipient) {
    dispatcher.sendAlertEmail(EmailTemplate.deleteRecipientAlertText(recipient))  
  }

} 



object EmailTemplate {

  def subjectPrefix = "Wish: "

  def footer = {
    """


      Sent by Wish.
      Host: %s
    """.format(EmailConfiguration.hostname)
  }


  def registrationText(recipient: Recipient) = {
    ("New registration", "Recipient " + recipient.username + " has registered with Wish")
  }
  

  def deleteRecipientAlertText(recipient: Recipient) = {
    ("Recipient deleted",
      """

        Recipient %s has been deleted from Wish.


      """.format(recipient.username))
  }

  def deleteRecipientNotificationText(recipient: Recipient) = {
    ("Recipient deleted",
      """
         Recipient %s has been deleted from Wish.

         We are sorry to see you leave.

      """.format(recipient.username) )
  }

  def newPasswordText(recipient: Recipient, newPassword: String): (String, String) = {
    ("Password reset",
      """
        Your new password is : %s

        If you didn't request this password reset for Wish, please let us know at %s
      """.format(newPassword,EmailConfiguration.hostname))
  }


  def changePasswordText(recipient: Recipient): (String, String) = {
    ("Password changed",
      """
        Your password for Wish has just been changed.

        If you didn't request this password change for Wish, please let us know at %s
      """.format(EmailConfiguration.hostname))
  }

  def emailVerificationText(username: String, verificationUrl: String): (String, String) = {
    ("Please verify your email address",
      """
        Hi, welcome to Wish.

        Please verify your email address by going to this website:
        %s


        If you didn't register with Wish, please let us know at %s
      """.format(verificationUrl,EmailConfiguration.hostname))
  }
  
}