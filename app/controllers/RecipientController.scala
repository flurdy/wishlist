package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._


object RecipientController extends Controller with Secured {


	def showProfile(username:String) = Action {  implicit request =>
    val recipient = Recipient.findByUsername(username).get
    val wishlists = Wishlist.findWishlistsByUsername(username)
    Ok(views.html.recipient.profile(recipient,wishlists))
  }

  def showEditRecipient(username:String) = withCurrentRecipient { currentRecipient => implicit request =>
    val recipient = Recipient.findByUsername(username).get
    Ok(views.html.recipient.editrecipient(recipient))
  }

  def showDeleteRecipient(username:String) = withCurrentRecipient { currentRecipient => implicit request =>
    val recipient = Recipient.findByUsername(username).get
    Ok(views.html.recipient.deleterecipient(recipient))
  }


  def deleteRecipient(username:String) = withCurrentRecipient { currentRecipient => implicit request =>
    val recipient = Recipient.findByUsername(username).get
    recipient.delete
    Redirect(routes.Application.index()).flashing("messageWarning" -> "Recipient deleted")
  }


  def updateRecipient(username:String)  = withCurrentRecipient { currentRecipient => implicit request =>
    val recipient = Recipient.findByUsername(username).get

    Redirect(routes.RecipientController.showEditRecipient(username)).flashing("messageWarning" -> "Recipient updated")
  }




}





