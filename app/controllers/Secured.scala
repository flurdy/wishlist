package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.Play.current


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



  def isProfileRecipient(profileUsername:String)(f: (Recipient) => Request[AnyContent] => Result) = withCurrentRecipient {
    currentRecipient => implicit request =>
    Recipient.findByUsername(profileUsername) match {
      case Some(profileRecipient) => {
        if( currentRecipient == profileRecipient || currentRecipient.isAdmin ){

          f(currentRecipient)(request)

        } else {
          Results.Unauthorized(views.html.error.permissiondenied()(request.flash,Some(currentRecipient),analyticsDetails))
        }
      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient),analyticsDetails))
    }
  }



  def withWishlist(username:String,wishlistId:Long)(f: Wishlist => Request[AnyContent] => Result) = Action { implicit request =>
    Wishlist.findById(wishlistId) match {
      case Some(wishlist) => {
        if(wishlist.recipient.username == username) {

          f(wishlist)(request)

        } else {
          Logger.warn("Wishlist %d recipient is not %s".format(wishlistId,username))
          Results.NotFound(views.html.error.notfound()(request.flash,findCurrentRecipient(request.session),analyticsDetails))
        }

      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,findCurrentRecipient(request.session),analyticsDetails))
    }
  }


  def isEditorOfWishlist(username:String,wishlistId:Long)(f: => (Wishlist,Recipient) => Request[AnyContent] => Result) = withCurrentRecipient { currentRecipient => implicit request =>
     Wishlist.findById(wishlistId) match {
      case Some(wishlist) => {
        if(wishlist.recipient.username == username) {
          if( currentRecipient.canEdit(wishlist) ) {

            f(wishlist,currentRecipient)(request)

          } else {
            Logger.warn("Recipient %s is not a recipient of wishlist %d".format(currentRecipient.username,wishlistId))
            Results.Unauthorized(views.html.error.permissiondenied()(request.flash,Some(currentRecipient),analyticsDetails))
          }
        } else {
          Logger.warn("Wishlist %d recipient is not %s".format(wishlistId,username))
          Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient),analyticsDetails))
        }
      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient),analyticsDetails))
    }
  }



  def isEditorOfWish(username:String,wishlistId:Long,wishId:Long)(f: => (Wish,Wishlist,Recipient) => Request[AnyContent] => Result) = isEditorOfWishlist(username,wishlistId) { (wishlist,currentRecipient) => implicit request =>
    WishEntry.findByIds(wishId,wishlistId) match {
      case Some(wishEntry) => {
        if(wishlistId == wishEntry.wishlist.wishlistId.get){

          f(wishEntry.wish,wishlist,currentRecipient)(request)

        } else {
          Logger.warn("Wish %d is not a member of wishlist %d".format(wishId,wishlistId))
          Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient),analyticsDetails))
        }
      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient),analyticsDetails))
    }
  }

  implicit def analyticsDetails: Option[String] = Play.configuration.getString("analytics.id")


  def withWish(username:String,wishlistId:Long,wishId:Long)(f: => (Wish,Wishlist) => Request[AnyContent] => Result) = withWishlist(username,wishlistId) { wishlist => implicit request =>
    WishEntry.findByIds(wishId,wishlistId) match {
      case Some(wishEntry) => {
        if(wishlistId == wishEntry.wishlist.wishlistId.get){

          f(wishEntry.wish,wishlist)(request)

        } else {
          Logger.warn("Wish %d is not a member of wishlist %d".format(wishId,wishlistId))
          Results.NotFound(views.html.error.notfound()(request.flash,findCurrentRecipient(request.session),analyticsDetails))
        }
      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,findCurrentRecipient(request.session),analyticsDetails))
    }
  }


  def withWishAndCurrentRecipient(username:String,wishlistId:Long,wishId:Long)(f: => (Wish,Wishlist,Recipient) => Request[AnyContent] => Result) = withCurrentRecipient { currentRecipient => implicit request =>
    Wishlist.findById(wishlistId) match {
      case Some(wishlist) => {
        if(wishlist.recipient.username == username) {
          WishEntry.findByIds(wishId,wishlistId) match {
            case Some(wishEntry) => {
              if(wishlistId == wishEntry.wishlist.wishlistId.get){

                f(wishEntry.wish,wishlist,currentRecipient)(request)

              } else {
                Logger.warn("Wish %d is not a member of wishlist %d".format(wishId,wishlistId))
                Results.NotFound(views.html.error.notfound()(request.flash,findCurrentRecipient(request.session),analyticsDetails))
              }
            }
            case None => Results.NotFound(views.html.error.notfound()(request.flash,findCurrentRecipient(request.session),analyticsDetails))
          }
        } else {
          Logger.warn("Wishlist %d recipient is not %s".format(wishlistId,username))
          Results.NotFound(views.html.error.notfound()(request.flash,Some(currentRecipient),analyticsDetails))
        }
      }
      case None => Results.NotFound(views.html.error.notfound()(request.flash,findCurrentRecipient(request.session),analyticsDetails))
    }


  }


  def withJustWishAndCurrentRecipient(username:String,wishId:Long)(f: => (Wish,Recipient) => Request[AnyContent] => Result) = withCurrentRecipient { currentRecipient => implicit request =>

    Wish.findById(wishId) match {
      case Some(wish) => {
        if(wish.recipient.username == username) {

          f(wish,currentRecipient)(request)

        } else {
          Logger.warn("Wish %d recipient is not %s but %s".format(wishId,username,wish.recipient.username))
          Results.NotFound(views.html.error.notfound()(request.flash,findCurrentRecipient(request.session),analyticsDetails))
        }
      }
      case None => {
        Logger.warn("Wish %d  not found".format(wishId))
        Results.NotFound(views.html.error.notfound()(request.flash,findCurrentRecipient(request.session),analyticsDetails))
      }
    }
  }



}
