package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Results.{NotFound, Unauthorized}
import models._
import play.api.Play.current
import scala.concurrent.Future


class UsernameRequest[A](val username: Option[String], request: Request[A]) extends WrappedRequest[A](request)

class MaybeCurrentRecipientRequest[A](val currentRecipient: Option[Recipient], request: Request[A]) extends WrappedRequest[A](request){
   lazy val username = request.session.get(Security.username)
}

object UsernameAction extends
    ActionBuilder[UsernameRequest] with ActionTransformer[Request, UsernameRequest] {
  def transform[A](request: Request[A]) = Future.successful {
    new UsernameRequest(request.session.get(Security.username), request)
  }
}

object MaybeCurrentRecipientAction extends
    ActionBuilder[MaybeCurrentRecipientRequest] with ActionTransformer[Request, MaybeCurrentRecipientRequest] {
  def transform[A](request: Request[A]) = Future.successful {
    new MaybeCurrentRecipientRequest(request.session.get(Security.username).map(new Recipient(_)), request)
  }
}

object IsAuthenticatedAction extends ActionFilter[UsernameRequest] {
  def filter[A](input: UsernameRequest[A]) = Future.successful {
     if(input.username.isEmpty) Some(Unauthorized)
     else None
  }
}


trait Secured {

   implicit def requestToCurrentRecipient(implicit request: MaybeCurrentRecipientRequest[_]): Option[Recipient] = request.currentRecipient

   def CurrentRecipientAction = new ActionRefiner[UsernameRequest, MaybeCurrentRecipientRequest] {
      def refine[A](input: UsernameRequest[A]) = Future.successful {
         input.username.map( username =>
                  new MaybeCurrentRecipientRequest(Some(new Recipient(username)), input))
              .toRight(NotFound)
      }
   }

  implicit def findCurrentRecipient(implicit session: Session): Future[Option[Recipient]] = {
    session.get(Security.username) match {
      case None => Future.successful(None)
      case Some(sessionUsername) =>
         Future.successful( Some( new Recipient( sessionUsername )))
         // Recipient.findByUsername( sessionUsername )
    }
  }

  // def withCurrentRecipient(f: Recipient => Request[AnyContent] => Result) = { //} isAuthenticated {
  //  //  username => implicit request =>
  //  implicit request =>
  //     val recipient: Option[Recipient] = None
  //     // Recipient.findByUsername(username).map { recipient =>
  //     recipient.map { recipient =>
  //
  //       f(recipient)(request)
  //
  //     }.getOrElse(onUnauthenticated(request))
  //     // }.getOrElse(onUnauthenticated(request))
  // }


/*

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

*/

}
