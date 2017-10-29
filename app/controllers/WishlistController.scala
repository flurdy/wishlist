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
      ) verifying("Organiser is the recipient", fields => fields match {
         case (recipient,wishlistid,username) => {
            recipient != username
         }
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

   implicit def analyticsDetails: Option[String]

   implicit def wishlistRequestToCurrentRecipient(implicit request: WishlistRequest[_]): Option[Recipient] = request.currentRecipient

   def WishlistAction(wishlistId: Long) = new ActionRefiner[MaybeCurrentRecipientRequest, WishlistRequest] {
      def refine[A](input: MaybeCurrentRecipientRequest[A]) = {
         implicit val flash = input.flash
         implicit val currentRecipient = input.currentRecipient
         wishlistLookup.findWishlist(wishlistId) map {
            _.map { shallowWishlist =>
               new WishlistRequest(shallowWishlist, input)
            }.toRight(NotFound(views.html.error.notfound()))
         }
      }
   }

   def WishlistEditorAction = new ActionRefiner[WishlistRequest, WishlistRequest] {
      def refine[A](input: WishlistRequest[A]) = {
         implicit val flash = input.flash
         implicit val currentRecipient = input.currentRecipient
         input.currentRecipient match {
            case Some(recipient) =>
               recipient.canEdit(input.wishlist) map {
                  case true  =>
                     Right(input) // new WishlistAccessRequest( input.wishlist, recipient, input))
                  case false => Left(Unauthorized(views.html.error.permissiondenied()))
               }
            case None => Future.successful(Left(Unauthorized(views.html.error.permissiondenied())))
         }
      }
   }
}


@Singleton
class WishlistController @Inject() (val configuration: Configuration,
   val recipientLookup: RecipientLookup, val appConfig: ApplicationConfig)
(implicit val wishlistRepository: WishlistRepository,
      val wishRepository: WishRepository,
      val wishlistOrganiserRepository: WishlistOrganiserRepository,
      val wishlistLookup: WishlistLookup, val wishLookup: WishLookup,
      val wishLinkRepository: WishLinkRepository,
      val wishEntryRepository: WishEntryRepository,
      val recipientRepository: RecipientRepository,
      val reservationRepository: ReservationRepository)
extends Controller with Secured with WithAnalytics with WishForm with WishlistForm with WishlistActions with WishActions with WithLogging {


    def createWishlist(username: String) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction).async { implicit request =>
        editWishlistForm.bindFromRequest.fold(
            errors => {
              logger.warn("Create failed: " + errors)
              Future.successful(BadRequest(views.html.wishlist.createwishlist(errors)))
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
                          logger.warn("Not able to save new wishlist")
                             InternalServerError(views.html.wishlist.createwishlist(
                                editWishlistForm, errorMessage = Some("Not able to save new wishlist")))
                     }
                  case _ =>
                     logger.warn(s"Recipient ${username} can not create a wishlist for ${request.username}")
                     Future.successful(Unauthorized(views.html.error.permissiondenied()))
              }
          }
        )
    }


   def showEditWishlist(username: String, wishlistId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>

      val editForm = editWishlistForm.fill((request.wishlist.title,request.wishlist.description))

      request.wishlist.findOrganisers flatMap { organisers =>
         request.wishlist.inflate map { wishlist =>
            Ok(views.html.wishlist.editwishlist(wishlist, editForm, organisers, addOrganiserForm))
         }
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
            andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>
      request.wishlist.inflate map { wishlist =>
         Ok(views.html.wishlist.deletewishlist(wishlist))
      }
   }

    def deleteWishlist(username: String, wishlistId: Long)     = removeWishlist(username, wishlistId)

    def alsoDeleteWishlist(username: String, wishlistId: Long) = removeWishlist(username, wishlistId)

    private def removeWishlist(username: String, wishlistId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
         andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>
       request.wishlist.delete.map {
          case true =>
             Redirect(routes.Application.index())
                   .flashing("messageWarning" -> "Wishlist deleted")
          case false =>
             logger.error("Failed to delete wishlist")
             InternalServerError(views.html.error.error500())
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

       updateWishlistOrderForm.bindFromRequest.fold(
         errors => {
           logger.warn("Update order failed: " + errors)
           Future.successful(
              Redirect(routes.WishlistController.showWishlist(username,wishlistId))
                    .flashing("messageError" -> "Order update failed"))
         },
         listOrder => {
            logger.info("Updating wishlist's order: " + wishlistId)
            val zippedOrders = listOrder
                           .split(",")
                           .toList
                           .map(_.toInt)
                           .zipWithIndex

            val wishEntries = zippedOrders.map { case (wishId, index) =>
               wishEntryRepository.findByIds( wishId, wishlistId) map {
                  _.map(we => we.copy(ordinal = Some(index)))
               }
            }

            val sequencedEntries = Future.sequence( wishEntries ).map ( _.flatten )
            val updatedEntries   = sequencedEntries.map ( _.map ( _.updateOrdinal ) )
            val sequencedUpdates = updatedEntries.flatMap ( Future.sequence(_) )

            sequencedUpdates.map { _ =>
               Redirect(routes.WishlistController.showWishlist(username,wishlistId))
                     .flashing("message" -> "Wishlist updated")
            }
         }
       )
     }


   def addOrganiserToWishlist(username: String, wishlistId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
            andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>

      def editForm = editWishlistForm.fill((request.wishlist.title, request.wishlist.description))


      addOrganiserForm.bindFromRequest.fold(
        errors => {
            logger.warn("Add failed: " + errors)
            request.wishlist.findOrganisers.map { organisers =>
               BadRequest(views.html.wishlist.editwishlist(request.wishlist, editForm, organisers, errors))
            }
        }, {
         case (recipientUsername, wishlistId, organiserUsername) => {

            def organiserForm = addOrganiserForm.fill(recipientUsername, wishlistId, organiserUsername)

            recipientLookup.findRecipient(organiserUsername) flatMap {
               case Some(organiser) =>
                  logger.info(s"Adding organiser $organiserUsername to wishlist $wishlistId")
                  request.wishlist.isOrganiser(organiser) flatMap {
                     case true  =>
                        request.wishlist.findOrganisers.map { organisers =>
                           BadRequest(views.html.wishlist.editwishlist(request.wishlist, editForm, organisers, organiserForm))
                                 .flashing("messageWarning" -> "Organiser is already an organiser")
                        }
                     case false =>
                        request.wishlist.addOrganiser(organiser) map { _ =>
                           Redirect(routes.WishlistController.showEditWishlist(username, wishlistId))
                                 .flashing ("messageSuccess" -> "Organiser added")
                        }
                  }
               case _ =>
                  logger.info(s"Organiser [$organiserUsername] not found")
                  request.wishlist.findOrganisers.map { organisers =>
                     BadRequest(views.html.wishlist.editwishlist(request.wishlist, editForm, organisers, organiserForm))
                           .flashing("messageWarning" -> "Organiser not found")
                  }
            }
         }
      }
    )
  }


  def alsoRemoveOrganiserFromWishlist(username: String, wishlistId: Long, organiserUsername: String) =
     removeOrganiserFromWishlist(username, wishlistId, organiserUsername)

  def removeOrganiserFromWishlist(username: String, wishlistId: Long, organiserUsername: String) =
    (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
          andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>

      logger.info(s"Removing organiser $organiserUsername from wishlist $wishlistId")
      recipientLookup.findRecipient(organiserUsername) flatMap {
         case Some(organiser) =>
            request.wishlist.removeOrganiser(organiser) map { _ =>
               Redirect(routes.WishlistController.showEditWishlist(username, wishlistId))
                  .flashing("messageRemoved" -> "Organiser removed")
            }
         case _ => Future.successful( NotFound(views.html.error.notfound()) )
      }
   }
}
