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

trait WishForm {

   val editWishlistForm = Form(
      tuple(
         "title" -> nonEmptyText(maxLength = 50,minLength = 2 ),
         "description" -> optional(text(maxLength = 2000))
      )
   )

   val simpleAddWishForm = Form (
      single(
         "title" -> text(maxLength = 50, minLength = 2 )
      )
   )


   val searchForm = Form {
      single(
         "term" -> optional(text(maxLength = 99))
      )
   }
}

class WishlistRequest[A](val wishlist: Wishlist, request: MaybeCurrentRecipientRequest[A]) extends WrappedRequest[A](request){
   lazy val username = request.username
   lazy val currentRecipient: Option[Recipient] = request.currentRecipient
   lazy val maybeRecipient: MaybeCurrentRecipientRequest[A] = request
}

class WishRequest[A](val wish: Wish, request: MaybeCurrentRecipientRequest[A]) extends WrappedRequest[A](request){
   lazy val username = request.username
   lazy val currentRecipient: Option[Recipient] = request.currentRecipient
}

class WishlistAccessRequest[A](val wishlist: Wishlist, currentRecipient: Recipient, request: WishlistRequest[A]) extends WrappedRequest[A](request){
   lazy val username = request.username
   val currentRecipienter = currentRecipient
   lazy val possibleCurrentRecipient = request.currentRecipient
}

// object WishlistEditorAction extends ActionFilter[WishlistRequest] {
//    def filter[A](input: WishlistRequest[A]) = Future.successful {
//       input.currentRecipient match {
//          case Some(recipient) if recipient.canEdit(input.wishlist) => None
//          case _ => Some(Forbidden)
//       }
//    }
// }


@Singleton
class WishController @Inject() (val configuration: Configuration, val recipientLookup: RecipientLookup)
extends Controller with Secured with WithAnalytics with WishForm {

   implicit def wishlistRequestToCurrentRecipient(implicit request: WishlistRequest[_]): Option[Recipient] = request.currentRecipient

   implicit def wishlistAccessRequestToCurrentRecipient(implicit request: WishlistAccessRequest[_]): Option[Recipient] = request.possibleCurrentRecipient

   def WishlistAction(wishlistId: Long) = new ActionRefiner[MaybeCurrentRecipientRequest, WishlistRequest] {
      def refine[A](input: MaybeCurrentRecipientRequest[A]) = Future.successful {
         Some(new Wishlist(wishlistId, input.currentRecipient.get)).map( wishlist =>
                  new WishlistRequest(wishlist, input))
              .toRight(NotFound)
      }
   }

   def WishWishlistAction(wishId: Long) = new ActionRefiner[WishlistRequest, WishRequest] {
      def refine[A](input: WishlistRequest[A]) = Future.successful {
         Some(new Wish(wishId, input.currentRecipient.get)).map( wish =>
                  new WishRequest(wish, input.maybeRecipient))
              .toRight(NotFound)
      }
   }

   def WishAction(wishId: Long) = new ActionRefiner[MaybeCurrentRecipientRequest, WishRequest] {
      def refine[A](input: MaybeCurrentRecipientRequest[A]) = Future.successful {
         Some(new Wish(wishId, input.currentRecipient.get)).map( wish =>
                  new WishRequest(wish, input))
              .toRight(NotFound)
      }
   }

   def WishlistEditorAction = new ActionRefiner[WishlistRequest, WishlistAccessRequest] {
      def refine[A](input: WishlistRequest[A]) = Future.successful {
         input.currentRecipient.filter(_.canEdit(input.wishlist)).map { currentRecipient =>
            new WishlistAccessRequest( input.wishlist, currentRecipient, input)
         }.toRight(Forbidden)
      }
   }


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

    val addOrganiserForm = Form {
      tuple(
        "recipient" -> text(maxLength = 180,minLength = 2 ),
        "wishlistid" -> number,
        "username" -> text(maxLength = 180,minLength = 2 )
      )  verifying("Organiser not found", fields => fields match {
        case (recipient,wishlistid,username) => {
          Recipient.findByUsername(username.trim).isDefined
        }
      }) verifying("Organiser is the recipient", fields => fields match {
        case (recipient,wishlistid,username) => {
          recipient != username
        }
      }) verifying("Recipient already an organiser", fields => fields match {
        case (recipient,wishlistid,username) => {
          Wishlist.findById(wishlistid) match {
            case Some(wishlist) => {
              Recipient.findByUsername(username.trim) match {
                case Some(recipient) =>  !wishlist.isOrganiser(recipient)
                case None => true
              }
            }
            case None => true
          }
        }
      })
    }
}

class WishController extends Controller with Secured {
  import WishController._
*/


    def createWishlist(username: String) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction).async { implicit request =>
        editWishlistForm.bindFromRequest.fold(
            errors => {
              Logger.warn("Create failed: " + errors)
              Future.successful(BadRequest)
              // (views.html.wishlist.createwishlist(errors))
            },
           titleForm => {
             request.currentRecipient match {
                case Some(currentRecipient) if request.username.contains(username) =>
                   Logger.info(s" $username is creating a new wishlist: ${titleForm._1}")
                   new Wishlist(title = titleForm._1.trim, recipient = currentRecipient)
                     .save
                     .map{ newWishlist =>
                        newWishlist.flatMap(_.wishlistId)
                                   .fold[Result]{
                                     throw new IllegalStateException("Save wishlist failed")
                                   }{ wishlistId =>
                              Redirect(
                                 routes.WishController.showWishlist(username, wishlistId))
                              .flashing("messageSuccess" -> "Wishlist created")
                        }
                     }
                  case _ =>
                     Logger.warn(s"Username can not create a wishlist for ${request.username}")
                     Future.successful(Unauthorized) // (views.html.error.permissiondenied())
              }
          }
        )
    }

 /*


  def showEditWishlist(username:String,wishlistId:Long) =  isEditorOfWishlist(username,wishlistId) { (wishlist,currentRecipient) => implicit request =>
    val editForm = editWishlistForm.fill((wishlist.title,wishlist.description))
    val organisers = wishlist.findOrganisers
    Ok(views.html.wishlist.editwishlist(wishlist, editForm,organisers,addOrganiserForm))
  }


    def updateWishlist(username:String,wishlistId:Long) = isEditorOfWishlist(username,wishlistId) { (wishlist,currentRecipient) => implicit request =>
      editWishlistForm.bindFromRequest.fold(
        errors => {
          Logger.warn("Update failed: " + errors)
          val organisers = wishlist.findOrganisers
          BadRequest(views.html.wishlist.editwishlist(wishlist,errors,organisers,addOrganiserForm))
        },
        editForm => {
          Logger.info("Updating wishlist: " + editForm)

          wishlist.copy(title=editForm._1,description=editForm._2).update

          Redirect(routes.WishController.showWishlist(username,wishlist.wishlistId.get)).flashing("message" -> "Wishlist updated")
        }
      )
    }


    */

    def deleteWishlist(username: String, wishlistId: Long) = removeWishlist(username, wishlistId)

    def alsoDeleteWishlist(username: String, wishlistId: Long) = removeWishlist(username, wishlistId)

    private def removeWishlist(username: String, wishlistId: Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
         andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>
       request.wishlist.delete.map{
          case true => Redirect(routes.Application.index()).flashing("messageWarning" -> "Wishlist deleted")
          case false => throw new IllegalStateException("Failed to delete wishlist")
       }
    }

   def redirectToShowWishlist(username: String, wishlistId: Long) = Action {
      Redirect(routes.WishController.showWishlist(username, wishlistId))
   }

   def showWishlist(username: String, wishlistId: Long) =
     (UsernameAction andThen MaybeCurrentRecipientAction
        andThen WishlistAction(wishlistId)).async { implicit request =>
   //  def showWishlist(username:String,wishlistId:Long) =  withWishlist(username,wishlistId) { wishlist => implicit request =>
      val gravatarUrl = "" // recipientGravatarUrl(request.wishlist)
      findCurrentRecipient map { currentRecipient =>
         currentRecipient match {
           case Some(recipient) => {
             val wishes = Seq( new Wish(1111, "A handbag", recipient))// Wishlist.findWishesForWishlist(request.wishlist)
             if( recipient.canEdit(request.wishlist) ) {
                Ok(views.html.wishlist.showwishlist(request.wishlist, wishes, simpleAddWishForm, gravatarUrl))
               // val editableWishlists = recipient.findEditableWishlists.filter( _ != request.wishlist )
               // Ok (views.html.wishlist.showeditwishlist(wishlist,wishes,simpleAddWishForm,gravatarUrl,editableWishlists))
             } else {
               Ok(views.html.wishlist.showwishlist(request.wishlist, wishes, simpleAddWishForm, gravatarUrl))
             }
           }
           case None => Ok // (views.html.wishlist.showwishlist(wishlist,wishes,simpleAddWishForm,gravatarUrl))
         }
      }
    }
/*

    def showConfirmDeleteWishlist(username:String,wishlistId:Long) = isEditorOfWishlist(username,wishlistId) { (wishlist,currentRecipient) => implicit request =>
        Ok(views.html.wishlist.deletewishlist(wishlist))
    }

    */


    def search =
     (UsernameAction andThen CurrentRecipientAction).async { implicit request =>
        searchForm.bindFromRequest.fold(
            errors => {
              Logger.warn("Update failed: " + errors)
             Future.successful( BadRequest ) // error page
            },
            term => {
                val wishlists = term match {
                    case None => List() //  Wishlist.findAll
                    case Some(searchTerm) => List() // Wishlist.searchForWishlistsContaining(searchTerm)
                }
                Future.successful( Ok(views.html.wishlist.listwishlists(wishlists,searchForm.fill(term),editWishlistForm)) )
            }
        )
   }


   // private def recipientGravatarUrl(wishlist:Wishlist) = RecipientController.gravatarUrl(wishlist.recipient)


   def addWishToWishlist(username:String,wishlistId:Long) =
     (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
           andThen WishlistAction(wishlistId) andThen WishlistEditorAction).async { implicit request =>
        simpleAddWishForm.bindFromRequest.fold(
          errors => {
              Logger.warn("Add failed: " + errors)
              request.wishlist.findWishes.map{ wishes =>
                 BadRequest(views.html.wishlist.showwishlist(request.wishlist,wishes,errors, "")) //recipientGravatarUrl(wishlist)))
              }
          },
          title => {
             for {
               wish <- new Wish(title, request.currentRecipienter).save
               _    <- wish.addToWishlist(request.wishlist)
            } yield Redirect(routes.WishController.showWishlist(username,wishlistId))
                                    .flashing("messageSuccess" -> "Wish added")
          }
        )
   }

/*


  def removeWishFromWishlist(username:String,wishlistId:Long,wishId:Long) = isEditorOfWish(username,wishlistId,wishId) { (wish,wishlist,currentRecipient) => implicit request =>

      wishlist.removeWish(wish)

      Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageWarning" -> "Wish deleted")
   }



  def updateWish(username:String,wishlistId:Long,wishId:Long) = isEditorOfWish(username,wishlistId,wishId) { (wish,wishlist,currentRecipient) => implicit request =>
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


  def reserveWish(username:String, wishlistId:Long, wishId:Long) =
      (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
             andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) ).async { implicit request =>

      request.wish.reserve(request.currentRecipient.get).map { _ =>
         Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageSuccess" -> "Wish reserved")
      }
  }


  def unreserveWish(username: String, wishlistId: Long, wishId: Long) =
    (UsernameAction andThen IsAuthenticatedAction andThen CurrentRecipientAction
          andThen WishlistAction(wishlistId) andThen WishWishlistAction(wishId) ).async { implicit request =>

   request.wish.unreserve.map { _ =>
      Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("message" -> "Wish reservation cancelled")
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


  def deleteLinkFromWish(username:String, wishlistId:Long, wishId:Long, linkId:Long) = isEditorOfWish(username,wishlistId,wishId) { (wish,wishlist,currentRecipient) => implicit request =>
    wish.findLink(linkId) match {
      case Some(url) => {
        wish.deleteLink(linkId)
        Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageWarning" -> "Link removed from wish")
      }
      case None => NotFound(views.html.error.notfound())
    }
  }


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
