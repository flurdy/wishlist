package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import scala.None

object WishController extends Controller with Secured {


    val editWishlistForm = Form(
        tuple(
          "title" -> nonEmptyText(maxLength = 180,minLength = 2 ),
          "description" -> optional(text(maxLength = 2000))
        )
    )

    val searchForm = Form {
        "term" -> optional(text(maxLength = 99))
    }   

    val simpleAddWishForm = Form {
        "title" -> text(maxLength = 180,minLength = 2 )
    }

    val editWishForm = Form(
      tuple(
        "title" -> nonEmptyText(maxLength = 180,minLength = 2 ),
        "description" -> optional(text(maxLength = 2000))
      )
    )


    def create(username:String) = withCurrentRecipient { currentRecipient => implicit request =>
        editWishlistForm.bindFromRequest.fold(
            errors => {
              Logger.warn("Create failed: " + errors)
              BadRequest(views.html.wishlist.createwishlist(errors))
            },
           titleForm => {
                Logger.info("New wishlist: " + titleForm._1)

                val wishlist = new Wishlist(None, titleForm._1.trim, None, currentRecipient).save

                Redirect(routes.WishController.showEditWishlist(username,wishlist.wishlistId.get)).flashing("messageSuccess" -> "Wishlist created")
            }
        )
    }


    def showEditWishlist(username:String,wishlistId:Long) =  withCurrentRecipient { currentRecipient => implicit request =>
      Wishlist.findById(wishlistId) match {
        case Some(wishlist) => {
          val editForm = editWishlistForm.fill((wishlist.title,wishlist.description))
          val wishes = Wishlist.findWishesForWishlist(wishlist)
          Ok(views.html.wishlist.editwishlist(wishlist, wishes, editForm))
        }
        case None => NotFound(views.html.error.notfound())
      }
    }


    def updateWishlist(username:String,wishlistId:Long) = withCurrentRecipient { currentRecipient => implicit request =>
      Wishlist.findById(wishlistId) match {
        case Some(wishlist) => {
          val wishes = Wishlist.findWishesForWishlist(wishlist)
          editWishlistForm.bindFromRequest.fold(
            errors => {
              Logger.warn("Update failed: " + errors)
              BadRequest(views.html.wishlist.editwishlist(wishlist,wishes,errors))
            }, 
            editForm => {
                Logger.info("Updating wishlist: " + editForm)

                wishlist.copy(title=editForm._1,description=editForm._2).update

                Redirect(routes.WishController.showEditWishlist(username,wishlist.wishlistId.get)).flashing("message" -> "Wishlist updated")
            }
            )
          }
        case None => NotFound(views.html.error.notfound())
      }
    }



    def deleteWishlist(username:String,wishlistId:Long) = withCurrentRecipient { currentRecipient => implicit request =>
      Wishlist.findById(wishlistId) match {
        case Some(wishlist) => {

          Logger.info("Deleting wishlist: " + wishlistId)

          Wishlist.findById(wishlistId).get.delete

          Redirect(routes.Application.index()).flashing("messageWarning" -> "Wishlist deleted")
        }
        case None => NotFound(views.html.error.notfound())
      }
    }


    def showWishlist(username:String,wishlistId:Long) =  withCurrentRecipient { currentRecipient => implicit request =>
      Wishlist.findById(wishlistId) match {
        case Some(wishlist) => {
          val wishes = Wishlist.findWishesForWishlist(wishlist)
          Ok(views.html.wishlist.showwishlist(wishlist,wishes,simpleAddWishForm))
        }
        case None => NotFound(views.html.error.notfound())
      }
    }


    def showConfirmDeleteWishlist(username:String,wishlistId:Long) = withCurrentRecipient { currentRecipient => implicit request =>
      Wishlist.findById(wishlistId) match {
        case Some(wishlist) => Ok(views.html.wishlist.deletewishlist(wishlist))
        case None => NotFound(views.html.error.notfound())
      }
    }


    def search = Action { implicit request =>
        searchForm.bindFromRequest.fold(
            errors => {
              Logger.warn("Update failed: " + errors)
              BadRequest
            }, 
            term => {
                val wishlists = term match {
                    case None => Wishlist.findAll
                    case Some(searchTerm) => Wishlist.searchForWishlistsContaining(searchTerm)
                }
                Ok(views.html.wishlist.listwishlists(wishlists,searchForm.fill(term),editWishlistForm))
            }
        )   
   }


    def addWishToWishlist(username:String,wishlistId:Long) = withCurrentRecipient { currentRecipient => implicit request =>
      Wishlist.findById(wishlistId) match {
        case Some(wishlist) => {
          simpleAddWishForm.bindFromRequest.fold(
            errors => {
                Logger.warn("Add failed: " + errors)
                val wishes = Wishlist.findWishesForWishlist(wishlist)
                BadRequest(views.html.wishlist.showwishlist(wishlist,wishes,errors))
            }, 
            title => {
                Wish(None,title,None,Some(wishlist)).save
                Redirect(routes.WishController.showWishlist(username,wishlist.wishlistId.get)).flashing("message" -> "Wish added")
            }
          )
        }
        case None => NotFound(views.html.error.notfound())
      }
   }


   def deleteWishFromWishlist(username:String,wishlistId:Long,wishId:Long) = withCurrentRecipient { currentRecipient => implicit request =>
    Wishlist.findById(wishlistId) match {
      case Some(wishlist) => {
        val wish = Wish.findById(wishId).get
        // TODO: validate wish is part of wishlist
        // TODO: Permission
          wish.delete

          Redirect(routes.WishController.showWishlist(username,wishlist.wishlistId.get)).flashing("messageWarning" -> "Wish deleted")
        }
        case None => NotFound(views.html.error.notfound())
      }
   }



  def updateWish(username:String,wishlistId:Long,wishId:Long) = withCurrentRecipient { currentRecipient => implicit request =>
    Wishlist.findById(wishlistId) match {
      case Some(wishlist) => {
        Wish.findById(wishId) match {
          case Some(wish) => {
            editWishForm.bindFromRequest.fold(
              errors => {
                Logger.warn("Update failed: " + errors)
                val wishes = Wishlist.findWishesForWishlist(wishlist)
                BadRequest(views.html.wishlist.showwishlist(wishlist,wishes,simpleAddWishForm))
              },
              editForm => {
                Logger.debug("Update title: " + editForm._1)
                wish.copy(title=editForm._1,description=editForm._2).update
                Redirect(routes.WishController.showWishlist(username,wishlistId)).flashing("messageSuccess" -> "Wish updated")
              }
            )
          }
          case None => NotFound(views.html.error.notfound())
        }
      }
      case None => NotFound(views.html.error.notfound())
    }
  }




}
