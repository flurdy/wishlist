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
      Recipient.findByUsername(username).map {
        recipient => f(recipient)(request)
      }.getOrElse(onUnauthenticated(request))
  }


  def isRecipientOf(username:String,wishlistId:Long)(f: => (Wishlist,Recipient) => Request[AnyContent] => Result) = withCurrentRecipient { currentRecipient => implicit request =>
    Wishlist.findById(wishlistId) match {
      case Some(wishlist) => {
        if(wishlist.recipient == currentRecipient && username == currentRecipient.username) {
          f(wishlist,currentRecipient)(request)
        } else {
            Logger.warn("Recipient %s can not a recipient of wishlist %d".format(currentRecipient.username,wishlistId))
          Results.Unauthorized(views.html.error.permissiondenied()(request.flash,Some(currentRecipient)))
        }
      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient)))
    }
  }


}
