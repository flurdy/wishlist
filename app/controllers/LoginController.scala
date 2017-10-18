package controllers

import javax.inject.{Inject, Singleton}
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import scala.util.matching.Regex
import models._
import repositories._


trait LoginForm {

   def ValidUsername: Regex

	val loginForm = Form(
	   tuple(
	      "username" -> nonEmptyText(minLength = 4, maxLength = 99),
	      "password" -> nonEmptyText(minLength = 4, maxLength = 99),
      	"source" -> optional(text)
      ) verifying("Log in failed. Username invalid", fields => fields match {
           case (username, _, _) =>  ValidUsername.unapplySeq(username).isDefined
      })
	)
}


@Singleton
class LoginController @Inject() (val configuration: Configuration, val recipientLookup: RecipientLookup)
   (implicit recipientRepository: RecipientRepository, val featureToggles: FeatureToggles)
extends Controller with Secured with WithAnalytics with LoginForm with RegisterForm with EmailAddressChecks with WithLogging {

   def redirectToLoginForm = Action { implicit request =>
      Redirect(routes.LoginController.showLoginForm())
   }

   def showLoginForm = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.login(loginForm))
   }

   def login = (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>

      def badLogin(username: String, errorMessage: String) =
         BadRequest(views.html.login(
               loginForm.fill((username, "", None)), Some(errorMessage)))
            .flashing("messageError" -> errorMessage)

      def loginFailed(username: String) =
         Future.successful(
            badLogin(username, "Log in failed. Username does not exist or password is invalid") )

      def notVerified(username: String) =
         badLogin(username, "Log in failed. Email not verified. Please check your email")

      def loginSuccess(username: String) =
         Redirect(routes.Application.index())
            .withSession("username" -> username.trim.toLowerCase)
            .flashing("message"->"You have logged in")

      loginForm.bindFromRequest.fold(
         errors => {
            logger.info("Log in failed:"+ errors)
            Future.successful( BadRequest(views.html.login(errors)) )
         },{
         case (username, password, source) =>
            recipientLookup.findRecipient(username.trim.toLowerCase) flatMap {
               case Some(recipient) =>
                  recipient.authenticate(password.trim) flatMap {
                     case true =>
                        recipient.isVerified map { isVerified =>
                           if(isVerified || FeatureToggle.EmailVerification.isDisabled()) {
                              logger.debug("Login success: " + username)
                              loginSuccess(username)
                           } else {
                              logger.warn("Login failed. Not verified: " + username)
                              notVerified(username)
                           }
                        }
                     case false =>
                        logger.warn("Login failed. Credentials not correct: " + username)
                        loginFailed(username)
                  }
               case _ =>
                  logger.warn("Login failed. Recipient not found: " + username)
                  loginFailed(username)
            }
         }
      )
   }

   def logout = Action {
      Redirect(routes.Application.index).withNewSession.flashing("message"->"You have been logged out")
   }

}
