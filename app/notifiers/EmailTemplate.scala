package notifiers

import models.Recipient


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

  def contactMessageText(name:String,email:String,username:Option[String],subject:Option[String],message:String,currentRecipient:Option[Recipient]) : (String,String) = {
    val actualSubject = subject.getOrElse("No subject entered")
    val actualUsername = username.getOrElse("No username entered")
    val actualRecipient = currentRecipient.map(recipient => recipient.username ).getOrElse("No current recipient")
    ("Contact message",
      """
        Current recipient: %s
        Name: %s
        Email: %s
        Username: %s

        Subject: %s

        Message:
%s
      """.format(actualRecipient,name,email,actualUsername,actualSubject,message))
  }

}