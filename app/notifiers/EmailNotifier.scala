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
    Logger.info("%s" .format(subjectAndBody._2))
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
    val verificationUrl = EmailConfiguration.hostname + "/recipient/" + recipient.username + "/verify/" + verificationHash +"/"
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

  def sendContactMessage(name:String,email:String,username:Option[String],subject:Option[String],message:String,currentRecipient:Option[Recipient]) {
    dispatcher.sendAlertEmail(EmailTemplate.contactMessageText(name,email,username,subject,message,currentRecipient))
  }

}