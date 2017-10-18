package controllers

import javax.inject.{Inject, Singleton}
import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
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
(implicit val wishlistRepository: WishlistRepository, val recipientRepository: RecipientRepository)
extends Controller with Secured with WithAnalytics with WishlistForm with WithLogging {

   def index = (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>
      request.currentRecipient match {
         case Some(currentRecipient) =>
            recipientLookup.findRecipient(currentRecipient.username) flatMap {
               case Some(recipient) =>
                  recipient.inflate.flatMap {
                     _.findAndInflateWishlists.map { wishlists =>
                        Ok(views.html.indexrecipient(editWishlistForm, wishlists))
                              .withSession(request.session)
                     }
                  }
               case _ => Future.successful( Redirect(routes.LoginController.logout) )
            }
         case None => Future.successful( Ok(views.html.indexanon()) )
      }
   }

   def redirectToIndex = Action { implicit request =>
      Redirect(routes.Application.index())
   }

   def about = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.about())
   }

}
