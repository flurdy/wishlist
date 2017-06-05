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
import scala.util.matching.Regex


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
extends Controller with Secured with WithAnalytics with LoginForm with RegisterForm {

   def redirectToLoginForm = Action { implicit request =>
      Redirect(routes.LoginController.showLoginForm())
   }

   def showLoginForm = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.login(loginForm))
   }

   def login = (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>

      def badLogin(username: String, errorMessage: String) =
         BadRequest(views.html.login(
               loginForm.fill((username, "", None))))
            .flashing("messageError" -> errorMessage)

      def loginFailed(username: String) =
         Future.successful(
            badLogin(username, "Log in failed. Username does not exist or password is invalid") )

      def notVerified(username: String) =
         badLogin(username, "Log in failed. Email not verified. Please check your email")

      def loginSuccess(username: String) =
         Redirect(routes.Application.index())
            .withSession("username" -> username)
            .flashing("message"->"You have logged in")

      loginForm.bindFromRequest.fold(
         errors => {
            Logger.info("Log in failed:"+ errors)
            Future.successful( BadRequest(views.html.login(errors)) )
         },{
         case (username, password, source) =>
            recipientLookup.findRecipient(username) flatMap {
               case Some(recipient) =>
                  recipient.authenticate(password) flatMap {
                     case true =>
                        recipient.isVerified map {
                           case true  =>
                              Logger.debug("Login success: " + username)
                              loginSuccess(username)
                           case false =>
                              Logger.warn("Login failed. Not verified: " + username)
                              notVerified(username)
                        }
                     case false =>
                        Logger.warn("Login failed. Credentials not correct: " + username)
                        loginFailed(username)
                  }
               case _ =>
                  Logger.warn("Login failed. Recipient not found: " + username)
                  loginFailed(username)
            }
         }
      )
   }
}
