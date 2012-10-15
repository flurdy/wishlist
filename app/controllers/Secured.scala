package controllers

import play.api._
import play.api.mvc._
import models._


trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def isAuthenticated(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthenticated) { username =>
      Action(request => f(username)(request))
    }
  }

  private def onUnauthenticated(request: RequestHeader) = {
    Results.Redirect(routes.Application.showLoginForm)
  }


  implicit def findCurrentRecipient(implicit session: Session): Option[Recipient] = {
    session.get(Security.username) match {
      case None => None
      case Some(sessionUsername) => Recipient.findByUsername( sessionUsername )
    }
  }


  def withCurrentRecipient(f: Recipient => Request[AnyContent] => Result) = isAuthenticated {
    username => implicit request =>
      Recipient.findByUsername(username).map { recipient => 

        f(recipient)(request)

      }.getOrElse(onUnauthenticated(request))
  }



  def isProfileRecipient(profileUsername:String)(f: (Recipient,Recipient) => Request[AnyContent] => Result) = withCurrentRecipient {
    currentRecipient => implicit request =>
    Recipient.findByUsername(profileUsername) match {
      case Some(profileRecipient) => {
        if( currentRecipient == profileRecipient ){
        
          f(profileRecipient,currentRecipient)(request)

        } else {
          Results.Unauthorized(views.html.error.permissiondenied()(request.flash,Some(currentRecipient)))
        }
      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient)))
    }
  }



  def withWishlist(wishlistId:Long)(f: (Wishlist,Recipient) => Request[AnyContent] => Result) = withCurrentRecipient {
    currentRecipient => implicit request =>
    Wishlist.findById(wishlistId) match {
      case Some(wishlist) => {

        f(wishlist,currentRecipient)(request) 

      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient)))
    }
  }


  def isRecipientOfWishlist(username:String,wishlistId:Long)(f: => (Wishlist,Recipient) => Request[AnyContent] => Result) = withWishlist(wishlistId) { (wishlist,currentRecipient) => implicit request =>
    if(wishlist.recipient == currentRecipient && username == currentRecipient.username) {

      f(wishlist,currentRecipient)(request)

    } else {
        Logger.warn("Recipient %s is not a recipient of wishlist %d".format(currentRecipient.username,wishlistId))
      Results.Unauthorized(views.html.error.permissiondenied()(request.flash,Some(currentRecipient)))
    }
  }


  def isRecipientOfWish(username:String,wishlistId:Long,wishId:Long)(f: => (Wish,Wishlist,Recipient) => Request[AnyContent] => Result) = isRecipientOfWishlist(username,wishlistId) { (wishlist,currentRecipient) => implicit request =>
    Wish.findById(wishId) match {
      case Some(wish) => {
        if(wishlist == wish.wishlist.get){ 

          f(wish,wishlist,currentRecipient)(request)

        } else {
          Logger.warn("Wish %d is not a member of wishlist %d".format(wishId,wishlistId))
          Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient)))
        }
      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient)))
    }
  }


}
