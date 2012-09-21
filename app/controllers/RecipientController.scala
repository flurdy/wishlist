package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._


object RecipientController extends Controller with Secured {
	
    def findWishlistsByRecipient(username:String) = Action { implicit request =>
        val wishlists = Wishlist.findWishlistsByUsername(username)
        Ok(views.html.recipient.profile(wishlists))
   }


	def showRecipient(username:String) = Action {

	}	

}





