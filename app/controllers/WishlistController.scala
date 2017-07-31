package controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc._
import play.api.mvc.Results.{Forbidden, NotFound}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import repositories._


trait WishlistForm {

   val editWishlistForm = Form(
      tuple(
         "title" -> nonEmptyText(maxLength = 50,minLength = 2 ),
         "description" -> optional(text(maxLength = 2000))
      )
   )

   val searchForm = Form {
      single(
         "term" -> optional(text(maxLength = 99))
      )
   }

   val addOrganiserForm = Form {
      tuple(
         "recipient"  -> text(maxLength = 180,minLength = 2 ),
         "wishlistid" -> number,
         "username"   -> text(maxLength = 180,minLength = 2 )
//      )  verifying("Organiser not found", fields => fields match {
//         case (recipient,wishlistid,username) => {
//            false // Recipient.findByUsername(username.trim).isDefined
//         }
//      }
      ) verifying("Organiser is the recipient", fields => fields match {
         case (recipient,wishlistid,username) => {
            recipient != username
         }
//      }) verifying("Recipient already an organiser", fields => fields match {
//         case (recipient,wishlistid,username) => {
//            false
//            /* Wishlist.findById(wishlistid) match {
//            case Some(wishlist) => {
//               Recipient.findByUsername(username.trim) match {
//               case Some(recipient) =>  !wishlist.isOrganiser(recipient)
//               case None => true
//               }
//            }
//            case None => true
//            }
//         } */
      })
   }

   val updateWishlistOrderForm = Form(
      single(
         "order" -> text(maxLength=500)
      )
   )
}

class WishlistRequest[A](val wishlist: Wishlist, request: MaybeCurrentRecipientRequest[A]) extends WrappedRequest[A](request){
   lazy val username = request.username
   lazy val currentRecipient: Option[Recipient] = request.currentRecipient
   lazy val maybeRecipient: MaybeCurrentRecipientRequest[A] = request
}

trait WishlistActions {

   implicit def wishlistLookup: WishlistLookup
   implicit def wishlistRepository: WishlistRepository
   implicit def recipientRepository: RecipientRepository

   implicit def wishlistRequestToCurrentRecipient(implicit request: WishlistRequest[_]): Option[Recipient] = request.currentRecipient

   def WishlistAction(wishlistId: Long) = new ActionRefiner[MaybeCurrentRecipientRequest, WishlistRequest] {
      def refine[A](input: MaybeCurrentRecipientRequest[A]) =
         wishlistLookup.findWishlist(wishlistId) map {
            _.map { shallowWishlist =>
               new WishlistRequest(shallowWishlist, input)
            }.toRight(NotFound)
         }
   }

   def WishlistEditorAction = new ActionRefiner[WishlistRequest, WishlistRequest] {
      def refine[A](input: WishlistRequest[A]) =
         input.currentRecipient match {
            case Some(recipient) =>
               recipient.canEdit(input.wishlist) map {
                  case true  =>
                     Right(input) // new WishlistAccessRequest( input.wishlist, recipient, input))
                  case false => Left(Forbidden)
               }
            case None => Future.successful(Left(Forbidden))
         }
   }
}


@Singleton
class WishlistController @Inject() (val configuration: Configuration,
   val recipientLookup: RecipientLookup)
(implicit val wishlistRepository: WishlistRepository, val wishlistLookup: WishlistLookup,
      val wishLookup: WishLookup, val wishLinkRepository: WishLinkRepository,
      val wishEntryRepository: WishEntryRepository, val recipientRepository: RecipientRepository)
extends Controller with Secured with WithAnalytics with WishForm with WishlistForm with WishlistActions with WishActions with WithLogging {


    def createWishlist(username: String) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction).async { implicit request =>
        editWishlistForm.bindFromRequest.fold(
            errors => {
              logger.warn("Create failed: " + errors)
              Future.successful(BadRequest)
              // TODO (views.html.wishlist.createwishlist(errors))
            },
           titleForm => {
             request.currentRecipient match {
                case Some(currentRecipient) if request.username.contains(username) =>
                   logger.info(s" $username is creating a new wishlist: ${titleForm._1}")
                   new Wishlist(title = titleForm._1.trim, recipient = currentRecipient)
                     .save
                     .map {
                        case Right(newWishlist) =>
                           newWishlist.wishlistId.fold{
                              throw new IllegalStateException("A new wishlist must have an id")
                           }{ wishlistId =>
                              Redirect(routes.WishlistController.showWishlist(username, wishlistId))
                                 .flashing("messageSuccess" -> "Wishlist created")
                           }
                        case _ =>
                           throw new IllegalStateException("Not able to save new wishlist")
                     }
                  case _ =>
                     logger.warn(s"Recipient ${username} can not create a wishlist for ${request.username}")
                     Future.successful(Unauthorized) // (views.html.error.permissiondenied())
              }
          }
        )
    }


   def showEditWishlist(username: String, wishlistId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>

      val editForm = editWishlistForm.fill((request.wishlist.title,request.wishlist.description))

      request.wishlist.findOrganisers map { organisers =>
         Ok(views.html.wishlist.editwishlist(request.wishlist, editForm, organisers, addOrganiserForm))
      }
   }

   def redirectoShowEditWishlist(username: String, wishlistId: Long) = Action {
      Redirect(routes.WishlistController.showEditWishlist(username, wishlistId))
   }

   def alsoUpdateWishlist(username: String, wishlistId: Long) = updateWishlist(username, wishlistId)

   def updateWishlist(username: String, wishlistId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>

      editWishlistForm.bindFromRequest.fold(
         errors => {
            logger.warn("Update failed: " + errors)
            request.wishlist.findOrganisers map { organisers =>
               BadRequest(views.html.wishlist.editwishlist(request.wishlist, errors, organisers, addOrganiserForm))
            }
        }, {
         case (title, description) => {
            logger.info("Updating wishlist: " + wishlistId)

            request.wishlist.copy(title = title, description = description).update map { wishlist =>
               Redirect(routes.WishlistController.showWishlist(username, wishlistId))
                     .flashing("message" -> "Wishlist updated")
            }
         }
        }
      )
   }


   def showConfirmDeleteWishlist(username: String, wishlistId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishlistEditorAction) { implicit request =>
      Ok(views.html.wishlist.deletewishlist(request.wishlist))
   }

    def deleteWishlist(username: String, wishlistId: Long)     = removeWishlist(username, wishlistId)

    def alsoDeleteWishlist(username: String, wishlistId: Long) = removeWishlist(username, wishlistId)

    private def removeWishlist(username: String, wishlistId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
         andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>
       request.wishlist.delete.map {
          case Right(_) =>
             Redirect(routes.Application.index())
                   .flashing("messageWarning" -> "Wishlist deleted")
          case Left(e) =>
             logger.error("Failed to delete wishlist",e)
             InternalServerError("Failed to delete wishlist")
       }
    }

   def redirectToShowWishlist(username: String, wishlistId: Long) = Action {
      Redirect(routes.WishlistController.showWishlist(username, wishlistId))
   }

   def showWishlist(username: String, wishlistId: Long) =
     (UsernameAction andThen MaybeCurrentRecipientAction
        andThen WishlistAction(wishlistId)).async { implicit request =>

      val gravatarUrl = None // recipientGravatarUrl(request.wishlist)

      request.wishlist.inflate.flatMap { wishlist =>
         wishlist.findWishes flatMap { wishes =>
            def showWishlist =
               Ok(views.html.wishlist.showwishlist(
                  wishlist, wishes, simpleAddWishForm, gravatarUrl))

            def showEditWishlist(editableWishlists: List[Wishlist]) =
               Ok(views.html.wishlist.showeditwishlist(
                  wishlist, wishes, simpleAddWishForm, gravatarUrl, editableWishlists))

            findCurrentRecipient flatMap {
               case Some(currentRecipient) =>
                  currentRecipient.canEdit(wishlist) flatMap {
                     case true  =>
                        currentRecipient.findEditableWishlists map { editableWishlists =>
                           showEditWishlist(editableWishlists.filter(_ != request.wishlist))
                        }
                     case false => Future.successful(showWishlist)
                  }
               case _ => Future.successful(showWishlist)
            }
         }
      }
   }


    def search =
     (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>
        logger.debug("searching")
        searchForm.bindFromRequest.fold(
            errors => {
               logger.warn("search form error")
               Future.successful( BadRequest(views.html.wishlist.listwishlists(List(), searchForm, editWishlistForm) ) )
            },
            term => {
               for {
                  wishlists <- term.fold(wishlistRepository.findAll)( t => wishlistRepository.searchForWishlistsContaining(t))
                  wishlistsFound <- Future.sequence( wishlists.map ( _.inflate ) )
               } yield
                  Ok(views.html.wishlist.listwishlists(wishlistsFound, searchForm.fill(term), editWishlistForm))
            }
        )
   }


   // private def recipientGravatarUrl(wishlist:Wishlist) = RecipientController.gravatarUrl(wishlist.recipient)



   def updateWishlistOrder(username: String, wishlistId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>

       // val wishes = Wishlist.findWishesForWishlist(wishlist)
       updateWishlistOrderForm.bindFromRequest.fold(
         errors => {
           logger.warn("Update order failed: " + errors)
           Future.successful(
              Redirect(routes.WishlistController.showWishlist(username,wishlistId))
                    .flashing("messageError" -> "Order update failed"))
         },
         listOrder => {
           logger.info("Updating wishlist's order: " + wishlistId)

           var ordinalCount = 1;
           listOrder.split(",") map { wishId =>
             wishEntryRepository.findByIds( wishId.toInt, wishlistId) map {
                _.map { wishEntry =>
                  wishEntry.copy( ordinal = Some(ordinalCount)).update map { _ =>
                     ordinalCount += 1
                  }
                }
             }
           }

           Future.successful(
              Redirect(routes.WishlistController.showWishlist(username,wishlistId))
                    .flashing("message" -> "Wishlist updated"))
         }
       )
     }


   def addOrganiserToWishlist(username: String, wishlistId: Long) = TODO

/*

def addOrganiserToWishlist(username:String,wishlistId:Long) = isEditorOfWishlist(username,wishlistId) { (wishlist,currentRecipient) => implicit request =>
      addOrganiserForm.bindFromRequest.fold(
        errors => {
            Logger.warn("Add failed: " + errors)
            val editForm = editWishlistForm.fill((wishlist.title,wishlist.description))
            val organisers = wishlist.findOrganisers
            BadRequest(views.html.wishlist.editwishlist(wishlist,editForm,organisers,errors))
        },
        organiserUsername => {
          Recipient.findByUsername(organiserUsername._3) match {
            case Some(organiser) => {
              Logger.info("Adding organiser %s to wishlist %d".format(organiser.username,wishlistId))

              wishlist.addOrganiser(organiser)

              Redirect(routes.WishController.showEditWishlist(username,wishlistId)).flashing("messageSuccess" -> "Organiser added")
            }
            case None => NotFound(views.html.error.notfound())
          }
        }
      )
 }

*/

 def alsoRemoveOrganiserFromWishlist(username: String, wishlistId: Long, organiserUsername: String) = TODO

 def removeOrganiserFromWishlist(username: String, wishlistId: Long, organiserUsername: String) = TODO

/*
def removeOrganiserFromWishlist(username:String,wishlistId:Long,organiserUsername:String) = isEditorOfWishlist(username,wishlistId) { (wishlist,currentRecipient) => implicit request =>
    Logger.info("Removing organiser %s from wishlist %d".format(organiserUsername,wishlistId))
    Recipient.findByUsername(organiserUsername) match {
      case Some(organiser) => {

        wishlist.removeOrganiser(organiser)

        Redirect(routes.WishController.showEditWishlist(username,wishlistId)).flashing("messageRemoved" -> "Organiser removed")
      }
      case None => NotFound(views.html.error.notfound())
    }
 }


*/

}
