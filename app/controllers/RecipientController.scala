package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._


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

  val changePasswordForm = Form(
    tuple(
      "password" -> nonEmptyText(minLength = 4, maxLength = 99),
      "confirm" -> nonEmptyText(minLength = 4, maxLength = 99)
    ) verifying("Passwords do not match", fields => fields match {
      case (password, confirmPassword) => {
        password.trim == confirmPassword.trim
      }
    })
  )

	def showProfile(username:String) = Action {  implicit request =>
    Recipient.findByUsername(username) match {
      case Some(recipient) => {
        val wishlists = Wishlist.findWishlistsByUsername(username)
        Ok(views.html.recipient.profile(recipient,wishlists))
      }
      case None => NotFound(views.html.error.notfound())
    }
  }

  def showEditRecipient(username:String) = withCurrentRecipient { currentRecipient => implicit request =>
    Recipient.findByUsername(username) match {
      case Some(recipient) => {
        val editForm = editRecipientForm.fill(recipient.username,recipient.username,recipient.fullname,recipient.email)
        Ok(views.html.recipient.editrecipient(recipient,editForm))
      }
      case None => NotFound(views.html.error.notfound())
    }
  }

  def showDeleteRecipient(username:String) = withCurrentRecipient { currentRecipient => implicit request =>
    Recipient.findByUsername(username) match {
      case Some(recipient) => {
        val recipient = Recipient.findByUsername(username).get
        Ok(views.html.recipient.deleterecipient(recipient))
      }
      case None => NotFound(views.html.error.notfound())
    }
  }


  def deleteRecipient(username:String) = withCurrentRecipient { currentEERecipient => implicit request =>
    Recipient.findByUsername(username) match {
      case Some(recipient) => {
        if( findCurrentRecipient == recipient ){
          recipient.delete
          Redirect(routes.Application.index()).flashing("messageWarning" -> "Recipient deleted")
        } else {
          Logger.warn("Recipient {} can not delete recipient {}".format(findCurrentRecipient.get.recipientId,recipient.recipientId))
          Redirect(routes.RecipientController.showDeleteRecipient(username)).flashing("messageError" -> "Permission denied")
        }
      }
      case None => NotFound(views.html.error.notfound())
    }
  }


  def updateRecipient(username:String)  = withCurrentRecipient { currentEERecipient => implicit request =>
    Recipient.findByUsername(username) match {
      case Some(recipient) => {
        editRecipientForm.bindFromRequest.fold(
          errors => {
            Logger.warn("Update failed: " + errors)
            BadRequest(views.html.recipient.editrecipient(recipient,errors))
          },
          editForm => {
            if( findCurrentRecipient == recipient ){
              val updatedRecipient = recipient.copy(
                fullname=editForm._3,
                email=editForm._4 )

              updatedRecipient.update

              Redirect(routes.RecipientController.showEditRecipient(username)).flashing("message" -> "Recipient updated")
            } else {
              Logger.warn("Recipient {} can not update recipient {}".format(findCurrentRecipient.get.recipientId,recipient.recipientId))
              Redirect(routes.RecipientController.showEditRecipient(username)).flashing("messageError" -> "Permission denied")
            }
          }
        )
      }
      case None => NotFound(views.html.error.notfound())
    }
  }




}





