package controllers

import javax.inject.{Inject, Singleton}
import play.api._
import play.api.mvc._
import play.api.mvc.Results.{Forbidden, NotFound}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import repositories._


trait WishForm {

   val simpleAddWishForm = Form (
      single(
         "title" -> text(maxLength = 50, minLength = 2 )
      )
   )
}

class WishRequest[A](val wish: Wish, request: MaybeCurrentRecipientRequest[A]) extends WrappedRequest[A](request){
   lazy val username = request.username
   lazy val currentRecipient: Option[Recipient] = request.currentRecipient
}


trait WishActions {

   implicit def wishLookup: WishLookup
   implicit def wishlistLookup: WishlistLookup
   implicit def wishlistRepository: WishlistRepository
   implicit def recipientRepository: RecipientRepository

   def WishWishlistAction(wishId: Long) = new ActionRefiner[WishlistRequest, WishRequest] {
      def refine[A](input: WishlistRequest[A]) = // Future.successful {
         wishLookup.findWishById(wishId) map {
            _.map ( new WishRequest(_, input.maybeRecipient) )
            .toRight(NotFound)
         }
//         input.currentRecipient
//            .map ( new Wish(wishId, _) ) // wrong!
//            .map ( new WishRequest(_, input.maybeRecipient) )
//            .toRight(NotFound)
//         Some(new Wish(wishId, input.currentRecipient.get)).map( wish =>
//                  new WishRequest(wish, input.maybeRecipient))
//              .toRight(NotFound)
//      }
   }

   def WishAction(wishId: Long) = new ActionRefiner[MaybeCurrentRecipientRequest, WishRequest] {
      def refine[A](input: MaybeCurrentRecipientRequest[A]) = Future.successful {
         Some(new Wish(wishId, input.currentRecipient.get)).map( wish =>
                  new WishRequest(wish, input))
              .toRight(NotFound)
      }
   }

}


@Singleton
class WishController @Inject() (val configuration: Configuration,
   val recipientLookup: RecipientLookup)
(implicit val wishlistRepository: WishlistRepository, val wishRepository: WishRepository, val wishEntryRepository: WishEntryRepository,
   val wishlistLookup: WishlistLookup, val wishLookup: WishLookup, val reservationRepository: ReservationRepository, val recipientRepository: RecipientRepository)
extends Controller with Secured with WithAnalytics with WishForm with WishActions with WishlistActions with WithLogging {


    /*
    val editWishForm = Form(
      tuple(
        "title" -> nonEmptyText(maxLength = 50,minLength = 2 ),
        "description" -> optional(text(maxLength = 2000))
      )
    )

    val updateWishlistOrderForm = Form(
      "order" -> text(maxLength=500)
    )

    val addLinkToWishForm = Form(
      "url" -> text(minLength=4,maxLength=250)
    )


    val moveWishForm = Form(
      "targetwishlistid" -> number
    )

    val ValidUrl= """^https?:\/\/[0-9a-zA-Z][^@]+$""".r

}
*/

   // private def recipientGravatarUrl(wishlist:Wishlist) = RecipientController.gravatarUrl(wishlist.recipient)


   def addWishToWishlist(username:String,wishlistId:Long) =
     (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
           andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>
        simpleAddWishForm.bindFromRequest.fold(
          errors => {
              logger.warn("Add failed: " + errors)
              request.wishlist.findWishes.map{ wishes =>
                 BadRequest(views.html.wishlist.showwishlist(request.wishlist,wishes,errors, None)) //recipientGravatarUrl(wishlist)))
              }
          },
          title => {
             new Wish(title, request.currentRecipienter).save.flatMap { wish =>
               wish.addToWishlist(request.wishlist).map { wishEntry =>
                  val url = routes.WishlistController.showWishlist(username,wishlistId)
                  Redirect(s"${url}?wish=${wish.wishId.get}")
                        .flashing("messageSuccess" -> "Wish added")
               }
            }
          }
        )
   }

   def alsoRemoveWishFromWishlist(username: String, wishlistId: Long, wishId: Long) = TODO

   def removeWishFromWishlist(username: String, wishlistId: Long, wishId: Long) = TODO

/*


  def removeWishFromWishlist(username:String,wishlistId:Long,wishId:Long) = isEditorOfWish(username,wishlistId,wishId) { (wish,wishlist,currentRecipient) => implicit request =>

      wishlist.removeWish(wish)

      Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageWarning" -> "Wish deleted")
   }

   */

   def alsoUpdateWish(username: String, wishlistId: Long, wishId: Long) = TODO

   def updateWish(username: String, wishlistId: Long, wishId: Long) = TODO

   /*

  def updateWish(username:String,wishlistId:Long,wishId:Long) = isEditorOfWish(username,wishlistId,wishId) { (wish,wishlist,currentRecipient) => implicit request =>
      editWishForm.bindFromRequest.fold(
      editWishForm.bindFromRequest.fold(
        errors => {
          Logger.warn("Update failed: " + errors)
          val wishes = wishlist.findWishes
           Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageError" -> "Wish update failed")
        },
        editForm => {
          Logger.debug("Update title: " + editForm._1)
          wish.copy(title=editForm._1,description=editForm._2).update
          Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageSuccess" -> "Wish updated")
        }
      )
  }


  */

   def updateWishlistOrder(username: String, wishlistId: Long) = TODO

   /*


     def updateWishlistOrder(username:String,wishlistId:Long) = isEditorOfWishlist(username,wishlistId) { (wishlist,currentRecipient) => implicit request =>

       // val wishes = Wishlist.findWishesForWishlist(wishlist)
       updateWishlistOrderForm.bindFromRequest.fold(
         errors => {
           Logger.warn("Update order failed: " + errors)
           Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageError" -> "Order update failed")
         },
         listOrder => {
           Logger.info("Updating wishlist's order: " + wishlistId)

           var ordinalCount = 1;
           listOrder.split(",") map { wishId =>
             WishEntry.findByIds(wishId.toInt,wishlistId) map { wishentry =>
               wishentry.copy(ordinal=Some(ordinalCount)).update
               ordinalCount += 1
             }
           }

           Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("message" -> "Wishlist updated")

         }
       )
     }

     */


  def reserveWish(username: String, wishlistId: Long, wishId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
             andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) ).async { implicit request =>
      request.currentRecipient match {
         case Some(r) if !r.isSame(request.wish.recipient) =>
            request.wish.reserve(request.currentRecipient.get).map { _ =>
               Redirect(routes.WishlistController.showWishlist(username,wishlistId))
                  .flashing("messageSuccess" -> "Wish reserved")
            }
         case Some(r) =>
            logger.warn("Can not reserve your own wish")
            Future.successful( Unauthorized )
         case _ =>
            logger.debug("Have to be logged in to reserve a wish")
            Future.successful( NotFound )
      }
  }


  def unreserveWish(username: String, wishlistId: Long, wishId: Long) =
    (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
          andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) ).async { implicit request =>

   request.wish.unreserve.map { _ =>
      Redirect(routes.WishlistController.showWishlist(username,wishlistId)).flashing("message" -> "Wish reservation cancelled")
      }
   }

   def unreserveWishFromProfile(username:String, wishId:Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
         andThen WishAction(wishId) ) { implicit request =>

            // withJustWishAndCurrentRecipient(username,wishId) { (wish,currentRecipient) => implicit request =>

      request.wish.reservation.map { reservation =>
         if(reservation.isReserver(request.currentRecipient.get)){
           request.wish.unreserve
         }
      }

      Redirect(routes.RecipientController.showProfile(request.username.get)).flashing("message" -> "Wish reservation cancelled")
   }


   def addLinkToWish(username:String, wishlistId: Long, wishId: Long) = TODO

   /*

   def addLinkToWish(username:String, wishlistId:Long, wishId:Long) =  isEditorOfWish(username,wishlistId,wishId) { (wish,wishlist,currentRecipient) => implicit request =>
     addLinkToWishForm.bindFromRequest.fold(
       errors => {
         Logger.warn("add to link failed")
         Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageError" -> "Link could not be added to wish")
       },
       url => {
         ValidUrl.findFirstIn(url.trim) match {
           case Some(_) => {
             wish.addLink(url)
             Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageSuccess" -> "Link added to wish")
           }
           case None => Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageError" -> "Invalid url. Make sure it starts with http or https?")
         }
       }
     )
   }

   */

   def alsoDeleteLinkFromWish(username: String, wishlistId: Long, wishId: Long, linkId: Long) = TODO

   def deleteLinkFromWish(username: String, wishlistId: Long, wishId: Long, linkId: Long) = TODO

/*
def deleteLinkFromWish(username:String, wishlistId:Long, wishId:Long, linkId:Long) = isEditorOfWish(username,wishlistId,wishId) { (wish,wishlist,currentRecipient) => implicit request =>
  wish.findLink(linkId) match {
    case Some(url) => {
      wish.deleteLink(linkId)
      Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageWarning" -> "Link removed from wish")
    }
    case None => NotFound(views.html.error.notfound())
  }
}

   */

   def moveWishToWishlist(username: String, wishlistId: Long, wishId: Long) = TODO

   /*

   def moveWishToWishlist(username:String,wishlistId:Long,wishId:Long) = isEditorOfWish(username,wishlistId,wishId) { (wish,wishlist,currentRecipient) => implicit request =>
     moveWishForm.bindFromRequest.fold(
       errors => {
         Logger.warn("move wish failed")
         Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageError" -> "Wish could not be moved")
       },
       targetWishlistId => {
         Wishlist.findById(targetWishlistId) match {
           case Some(targetWishlist) => {
             if( currentRecipient.canEdit(targetWishlist) ) {
               Logger.info("moving wish to %d".format(targetWishlist.wishlistId.get))

               wish.moveToWishlist(targetWishlist)

               Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("message" -> "Wish moved to other list")
             } else {
               Unauthorized(views.html.error.permissiondenied())
             }
           }
           case None => NotFound(views.html.error.notfound())
         }
       }
     )
   }

   */
}
