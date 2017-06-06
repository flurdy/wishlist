package controllers

// import play.api.Play.current
// import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import models._
// import notifiers._
// import java.math.BigInteger
// import java.security.SecureRandom
import scala.concurrent.Future


trait WithAnalytics {

   def configuration: Configuration

   implicit def analyticsDetails: Option[String] = configuration.getString("analytics.id")

}

@Singleton
class Application @Inject() (val configuration: Configuration)
extends Controller with Secured with WithAnalytics with WishForm {

   def index = Action.async { implicit request =>
      findCurrentRecipient map { implicit currentRecipient =>
         currentRecipient match {
         case Some(recipient) => {
            Logger.debug("yay already logged in")
            val wishlists: Seq[Wishlist] = Seq.empty // Wishlist.findByRecipient(recipient)
            // Ok(views.html.indexanon()).withSession( request.session )
            Ok(views.html.indexrecipient(
               editWishlistForm, wishlists)).withSession( request.session )
         }
         case None =>
            Logger.debug("not logged in")
            Ok(views.html.indexanon())
      }
   } }

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
