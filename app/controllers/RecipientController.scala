package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import notifiers._
import scravatar._


object RecipientController extends Controller with Secured {

  val ValidEmailAddress = """^[0-9a-zA-Z]([+-_\.\w]*[0-9a-zA-Z])*@([0-9a-zA-Z][-\w]*[0-9a-zA-Z]\.)+[a-zA-Z]{2,9}$""".r

  val ValidUsername= """^[0-9a-zA-Z_-]+$""".r


  val editRecipientForm = Form(
    tuple(
      "oldusername" -> nonEmptyText(maxLength = 99),
      "username" -> nonEmptyText(maxLength = 99),
      "fullname" -> optional(text(maxLength = 99)),
      "email" -> nonEmptyText(maxLength = 99)
    ) verifying("Email address is not valid", fields => fields match {
      case (oldusername, username, fullname, email) => {
        ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    })   verifying("Username is not valid. A to Z and numbers only. No spaces", fields => fields match {
      case (oldusername, username, fullname, email) => {
        ValidUsername.findFirstIn(username.trim).isDefined
      }
    })  verifying("Username is already taken", fields => fields match {
      case (oldusername, username, fullname, email) => {
        oldusername == username || !Recipient.findByUsername(username.trim).isDefined
      }
    })
  )


  def changePasswordForm(username:String) = Form(
    tuple(
      //"username" -> nonEmptyText(minLength = 4, maxLength = 99),
      "password" -> nonEmptyText(minLength = 4, maxLength = 99),
      "newpassword" -> nonEmptyText(minLength = 4, maxLength = 99),
      "confirm" -> nonEmptyText(minLength = 4, maxLength = 99)
    ) verifying("Passwords do not match", fields => fields match {
      case (password, newpassword, confirmPassword) => {
        newpassword.trim == confirmPassword.trim
      }
    }) verifying("Current password is invalid", fields => fields match {
      case (password, newpassword, confirmPassword) =>  Recipient.authenticate(username, password).isDefined
    })  
  )


  val resetPasswordForm = Form(
    tuple(
    "username" -> nonEmptyText(maxLength = 99),
    "email" -> nonEmptyText(maxLength = 99)
    ) verifying("Email address is not valid", fields => fields match {
      case (username, email) => {
        RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    }) verifying("Username is not valid. A to Z and numbers only", fields => fields match {
      case (username, email) => {
        RecipientController.ValidUsername.findFirstIn(username.trim).isDefined
      }
    }) verifying("Username and email does match or exist", fields => fields match {
      case (username, email) => {
        if(RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined &&
            RecipientController.ValidUsername.findFirstIn(username.trim).isDefined) {
          Recipient.findByUsernameAndEmail(username.trim,email.trim).isDefined
        } else {
          true
        }
      }
    }) 
  )

  def gravatarUrl(recipient:Recipient) = Gravatar(recipient.email).default(Monster).maxRatedAs(PG).size(100).avatarUrl

	def showProfile(username:String) = Action {  implicit request =>
    Recipient.findByUsername(username) match {
      case Some(recipient) => {        
        val wishlists = recipient.findWishlists
        val reservations = recipient.findReservations
        Ok(views.html.recipient.profile(recipient,wishlists,reservations,WishController.editWishlistForm,gravatarUrl(recipient)))
      }
      case None => NotFound(views.html.error.notfound())
    }
  }

  def showEditRecipient(username:String) = isProfileRecipient(username) { (profileRecipient) => implicit request =>
    val editForm = editRecipientForm.fill(profileRecipient.username,profileRecipient.username,profileRecipient.fullname,profileRecipient.email)
    Ok(views.html.recipient.editrecipient(profileRecipient,editForm))
  }

  def showDeleteRecipient(username:String) = isProfileRecipient(username)  { (profileRecipient) => implicit request =>
    Ok(views.html.recipient.deleterecipient(profileRecipient))    
  }


  def deleteRecipient(username:String) = isProfileRecipient(username)  { (profileRecipient) => implicit request =>
    
    profileRecipient.delete

    Redirect(routes.Application.index()).flashing("messageWarning" -> "Recipient deleted")
  }


  def updateRecipient(username:String)  = isProfileRecipient(username)  { (profileRecipient) => implicit request =>
    editRecipientForm.bindFromRequest.fold(
      errors => {
        Logger.warn("Update failed: " + errors)
        BadRequest(views.html.recipient.editrecipient(profileRecipient,errors))
      },
      editForm => {
        val updatedRecipient = profileRecipient.copy(
          fullname=editForm._3,
          email=editForm._4 )

        updatedRecipient.update

        Redirect(routes.RecipientController.showEditRecipient(username)).flashing("message" -> "Recipient updated")
      }
    )
  }


   def showResetPassword = Action { implicit request =>
    Ok(views.html.recipient.passwordreset(resetPasswordForm))
  } 

  def resetPassword = Action { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      errors => {
        BadRequest(views.html.recipient.passwordreset(errors))
      },
      resetForm => {
        Recipient.findByUsernameAndEmail(resetForm._1,resetForm._2) match { 
          case Some(recipient) => {
            Logger.info("Password reset requested for: " + recipient.recipientId)

            val newPassword = recipient.resetPassword
            
            EmailNotifier.sendPasswordResetEmail(recipient,newPassword)

            Redirect(routes.Application.index()).flashing("messageWarning" -> "Password reset and sent by email")
          }
          case None => {
            NotFound(views.html.error.notfound())   
          }
        }
      }
    )
  }



   def showChangePassword(username:String)  = isProfileRecipient(username) { (profileRecipient) => implicit request =>
    Ok(views.html.recipient.passwordchange(changePasswordForm(username)))
  } 

  def updatePassword(username:String)  = isProfileRecipient(username) { (profileRecipient) => implicit request =>
    changePasswordForm(username).bindFromRequest.fold(
      errors => {
        BadRequest(views.html.recipient.passwordchange(errors))
      },
      resetForm => {

        Logger.info("Password change requested for: " + profileRecipient.username)

        profileRecipient.updatePassword(resetForm._3)
        
        EmailNotifier.sendPasswordChangeEmail(profileRecipient)

        Redirect(routes.Application.showLoginForm
          ).withNewSession.flashing("messageWarning" -> "Password changed successfully. Please log in again")
      }
    )
  } 

}





