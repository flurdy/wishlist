package controllers

import javax.inject.{Inject, Singleton}
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import repositories._


trait WithAnalytics {

   def configuration: Configuration

   implicit def analyticsDetails: Option[String] = configuration.getString("analytics.id")

}

trait WithLogging {

   val logger: Logger = Logger(this.getClass())

}

@Singleton
class Application @Inject() (val configuration: Configuration, val recipientLookup: RecipientLookup)
(implicit val wishlistRepository: WishlistRepository)
extends Controller with Secured with WithAnalytics with WishForm with WithLogging {

   def index = (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>
      request.currentRecipient match {
         case Some(recipient) => {
            logger.debug("yay already logged in")
            recipient.findWishlists.map { wishlists =>
               Ok(views.html.indexrecipient(
                  editWishlistForm, wishlists)).withSession( request.session )
            }
         }
         case None =>
            logger.debug("not logged in")
            Future.successful( Ok(views.html.indexanon()) )
      }
   }


   def redirectToIndex = Action { implicit request =>
      Redirect(routes.Application.index())
   }

   def about = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.about())
   }

   def logout = Action {
      Redirect(routes.Application.index).withNewSession.flashing("message"->"You have been logged out")
   }

}
