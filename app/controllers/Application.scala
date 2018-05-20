package controllers

import javax.inject.Inject
import play.api._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import repositories._
import models._


trait WithLogging {

   val logger: Logger = Logger(this.getClass())

}


class Application @Inject()(cc: ControllerComponents, val recipientLookup: RecipientLookup, val appConfig: ApplicationConfig, usernameAction: UsernameAction, maybeCurrentRecipientAction: MaybeCurrentRecipientAction)
(implicit val executionContext: ExecutionContext, val wishlistRepository: WishlistRepository, val recipientRepository: RecipientRepository)
extends AbstractController(cc) with Secured with WithAnalytics with WishlistForm with WithLogging with WithAdsense {

   def index() = (usernameAction andThen maybeCurrentRecipientAction).async { implicit request =>
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

   def about = (usernameAction andThen maybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.about())
   }

}
