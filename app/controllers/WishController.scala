package controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc._
import play.api.mvc.Results.{NotFound, Unauthorized}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import repositories._


trait WishForm {

   val ValidUrl = """^https?:\/\/.+$""".r

   val simpleAddWishForm = Form (
      single(
         "title" -> text(maxLength = 50, minLength = 2 )
      )
   )

   val editWishForm = Form(
      tuple(
         "title" -> nonEmptyText(maxLength = 50,minLength = 2 ),
         "description" -> optional(text(maxLength = 2000))
      )
   )

   val addLinkToWishForm = Form(
      single (
         "url" -> text(minLength=4,maxLength=250)
      ) verifying("Invalid url format. Make sure it starts with http or https?", fields => fields match {
         case url => ValidUrl.findFirstIn(url.trim).isDefined
      })
   )

   val moveWishForm = Form(
      single (
         "targetwishlistid" -> number
      )
   )
}

class WishRequest[A](val wish: Wish, val wishlist: Option[Wishlist], request: MaybeCurrentRecipientRequest[A]) extends WrappedRequest[A](request){
   lazy val username = request.username
   lazy val currentRecipient: Option[Recipient] = request.currentRecipient
}

trait WishActions {

   implicit def wishLookup: WishLookup
   implicit def wishlistLookup: WishlistLookup
   implicit def wishlistRepository: WishlistRepository
   implicit def recipientRepository: RecipientRepository

   implicit def analyticsDetails: Option[String]

   def WishWishlistAction(wishId: Long) = new ActionRefiner[WishlistRequest, WishRequest] {
      def refine[A](input: WishlistRequest[A]) =
         wishLookup.findWishById(wishId) map { w =>
            implicit val flash = input.flash
            implicit val currentRecipient = input.currentRecipient
            w.map ( new WishRequest(_, Some(input.wishlist), input.maybeRecipient) )
            .toRight(NotFound(views.html.error.notfound()))
         }
   }

   def WishAction(wishId: Long) = new ActionRefiner[MaybeCurrentRecipientRequest, WishRequest] {
      def refine[A](input: MaybeCurrentRecipientRequest[A]) = Future.successful {
         implicit val flash = input.flash
         implicit val currentRecipient = input.currentRecipient
         Some(new Wish(wishId, input.currentRecipient.get)).map( wish =>
                  new WishRequest(wish, None, input))
              .toRight(NotFound(views.html.error.notfound()))
      }
   }

   def WishEditorAction(wishlistId: Long) = new ActionRefiner[WishRequest, WishRequest] {
      def refine[A](input: WishRequest[A]) = {
         implicit val flash = input.flash
         implicit val currentRecipient = input.currentRecipient
         input.currentRecipient match {
            case Some(recipient) =>
               wishlistLookup.findWishlist(wishlistId).flatMap {
                  case Some(wishlist) =>
                     recipient.canEdit(wishlist).map {
                        case true  => Right(input)
                        case false => Left(Unauthorized(views.html.error.permissiondenied()))
                     }
                  case None => Future.successful(Left(NotFound(views.html.error.notfound())))
               }
            case None => Future.successful(Left(Unauthorized(views.html.error.permissiondenied())))
         }
      }
   }
}


@Singleton
class WishController @Inject() (val configuration: Configuration,
   val recipientLookup: RecipientLookup, val appConfig: ApplicationConfig)
(implicit val wishlistRepository: WishlistRepository, val wishRepository: WishRepository,
      val wishEntryRepository: WishEntryRepository, val wishlistLookup: WishlistLookup,
      val wishLookup: WishLookup, val wishLinkRepository: WishLinkRepository,
      val reservationRepository: ReservationRepository, val recipientRepository: RecipientRepository, val featureToggles: FeatureToggles)
extends Controller with Secured with WithAnalytics with WishForm with WishActions with WishlistActions with WithLogging with WithGravatarUrl {

   def gravatarUrl(wishlist: Wishlist) = generateGravatarUrl(wishlist.recipient)

   implicit def requestToCurrentRecipient(implicit request: WishRequest[_]): Option[Recipient] = request.currentRecipient
    /*

    val updateWishlistOrderForm = Form(
      "order" -> text(maxLength=500)
    )

   */

   def addWishToWishlist(username:String,wishlistId:Long) =
     (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
           andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>
        simpleAddWishForm.bindFromRequest.fold(
          errors => {
              logger.warn("Add failed: " + errors)
              request.wishlist.findWishes.map{ wishes =>
                 BadRequest(views.html.wishlist.showwishlist(request.wishlist,wishes,errors, gravatarUrl(request.wishlist)))
              }
          },
          title => {
             request.currentRecipient match {
                case Some(currentRecipient) =>
                   new Wish(title, currentRecipient).save.flatMap { wish =>
                      wish.addToWishlist(request.wishlist).map { wishEntry =>
                         val url = routes.WishlistController.showWishlist(username, wishlistId)
                         Redirect(s"${url}?wish=${wish.wishId.get}")
                               .flashing("messageSuccess" -> "Wish added")
                      }
                   }
                case _ =>  Future.successful( NotFound(views.html.error.notfound()) )
             }
          }
        )
   }

   def alsoRemoveWishFromWishlist(username: String, wishlistId: Long, wishId: Long) = removeWishFromWishlist( username, wishlistId, wishId)

   def removeWishFromWishlist(username: String, wishlistId: Long, wishId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) andThen WishEditorAction(wishlistId) ).async { implicit request =>
         request.wishlist match {
            case Some(wishlist) =>
               wishlist.removeWish(request.wish).map { wishlist =>
                  Redirect(routes.WishlistController.showWishlist(username, wishlistId))
                     .flashing("messageWarning" -> "Wish deleted")
               }
            case _ => Future.successful(NotFound(views.html.error.notfound()))
         }
      }

   def alsoUpdateWish(username: String, wishlistId: Long, wishId: Long) = updateWish(username, wishlistId, wishId)

   def updateWish(username: String, wishlistId: Long, wishId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) andThen WishEditorAction(wishlistId) ).async { implicit request =>

         editWishForm.bindFromRequest.fold(
            errors => {
               logger.info(s"Bad form. Can not update wish [${wishId}] for [${username}]")
               Future.successful(
                  Redirect(routes.WishlistController.showWishlist(username,wishlistId))
                        .flashing("messageError" -> "Wish update failed"))
            }, {
            case (title, description) =>
               logger.info(s"Updating wish [${wishId}] for [${username}]")
               request.wish.copy ( title = title , description = description)
                           .update.map { wish =>
                  Redirect(routes.WishlistController.showWishlist(username,wishlistId))
                        .flashing("messageSuccess" -> "Wish updated")
               }
            }
         )
   }


  def reserveWish(username: String, wishlistId: Long, wishId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
             andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) ).async { implicit request =>
      request.currentRecipient match {
         case Some(r) if !r.isSame(request.wish.recipient) =>
            logger.info(s"Reserving wish $wishId by ${r.username} for ${request.wish.recipient.recipientId}")
            request.wish.reserve(request.currentRecipient.get).map { _ =>
               Redirect(routes.WishlistController.showWishlist(username,wishlistId))
                  .flashing("messageSuccess" -> "Wish reserved")
            }
         case Some(r) =>
            logger.warn("Can not reserve your own wish")
            Future.successful( Unauthorized(views.html.error.permissiondenied()) )
         case _ =>
            logger.debug("Have to be logged in to reserve a wish")
            Future.successful( NotFound(views.html.error.notfound()) )
      }
  }


  def unreserveWish(username: String, wishlistId: Long, wishId: Long) =
    (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
          andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) ).async { implicit request =>
       (request.currentRecipient, request.wish.reservation) match {
         case (Some(currentRecipient), Some(reservation)) =>
            currentRecipient.inflate.flatMap { thickerRecipient =>
               reservation.inflate.flatMap { thickerReservation =>
                  if( thickerReservation.isReserver(thickerRecipient) ){
                     logger.info(s"Unreserving wish $wishId reserved by ${currentRecipient.username} for ${request.wish.recipient.username}")
                     reservation.cancel.map { _ =>
                        Redirect(routes.WishlistController.showWishlist(username, wishlistId))
                              .flashing("message" -> "Wish reservation cancelled")
                     }
                  } else {
                     logger.warn("===== not reserver")
                     Future.successful(Unauthorized(views.html.error.permissiondenied()))
                  }
               }
            }
         case _ => Future.successful(NotFound(views.html.error.notfound()))
       }
   }

   def unreserveWishFromProfile(username:String, wishId:Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
         andThen WishAction(wishId) ).async { implicit request =>

      request.wish.reservation.fold[Future[Result]](Future.successful(NotFound)){ reservation =>
         if(reservation.isReserver(request.currentRecipient.get)){
           reservation.cancel.map{ _ =>
               Redirect(routes.RecipientController.showProfile(request.username.get))
                     .flashing("message" -> "Wish reservation cancelled")
           }
         } else Future.successful(Unauthorized(views.html.error.permissiondenied()))
      }
   }

   def addLinkToWish(username:String, wishlistId: Long, wishId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) andThen WishEditorAction(wishlistId) ).async { implicit request =>

     addLinkToWishForm.bindFromRequest.fold(
       errors => {
          logger.warn("add to link failed")
          Future.successful( Redirect(routes.WishlistController.showWishlist(username,wishlistId))
               .flashing("messageError" -> "Link could not be added to wish"))
       },
       url => {
          request.wish.addLink(url).map { _ =>
             Redirect(routes.WishlistController.showWishlist(username, wishlistId))
                   .flashing("messageSuccess" -> "Link added to wish")
          }
       }
     )
   }

   def alsoDeleteLinkFromWish(username: String, wishlistId: Long, wishId: Long, linkId: Long) =
      deleteLinkFromWish(username, wishlistId, wishId, linkId)

   def deleteLinkFromWish(username: String, wishlistId: Long, wishId: Long, linkId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) andThen WishEditorAction(wishlistId) ).async { implicit request =>

      request.wish.findLink(linkId) flatMap {
         case Some(link) =>
            link.delete map { _ =>
               Redirect(routes.WishlistController.showWishlist(username, wishlistId))
                     .flashing("messageWarning" -> "Link removed from wish")
            }
         case None => Future.successful( NotFound(views.html.error.notfound()))
      }
   }


   def moveWishToWishlist(username: String, wishlistId: Long, wishId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) andThen WishEditorAction(wishlistId) ).async { implicit request =>

      moveWishForm.bindFromRequest.fold(
         errors => {
            logger.warn("move wish failed")
            Future.successful(
               Redirect(routes.WishlistController.showWishlist(username,wishlistId))
                     .flashing("messageError" -> "Wish could not be moved"))
         },
         targetWishlistId => {
            request.currentRecipient match {
               case Some(currentRecipient) =>
                  wishlistLookup.findWishlist(targetWishlistId) flatMap {
                     case Some(targetWishlist) =>
                        currentRecipient.canEdit(targetWishlist) flatMap {
                           case true =>
                              logger.info(s"moving wish to ${targetWishlist.wishlistId}")
                              request.wish.moveToWishlist(targetWishlist).map { _ =>
                                 Redirect(routes.WishlistController.showWishlist(username, wishlistId))
                                       .flashing("message" -> "Wish moved to other list")
                              }
                           case false => Future.successful( Unauthorized(views.html.error.permissiondenied()) )
                        }
                     case None => Future.successful(NotFound(views.html.error.notfound()))
                  }
               case _ => Future.successful(InternalServerError(views.html.error.error500()))
            }
         }
      )
   }
}
