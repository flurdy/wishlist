package controllers

import javax.inject.{Inject, Singleton}
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import java.math.BigInteger
import java.security.SecureRandom


trait WithAnalytics {

   def configuration: Configuration

   implicit def analyticsDetails: Option[String] = configuration.getString("analytics.id")

}

@Singleton
class Application @Inject() (val configuration: Configuration)
extends Controller with Secured with WithAnalytics {


	val simpleRegisterForm = Form {
		"email" -> optional(text(maxLength = 99))
 	}	

 	val registerForm = Form(
    tuple(
      "username" -> nonEmptyText(maxLength = 99),
      "fullname" -> optional(text(maxLength = 99)),
      "email" -> nonEmptyText(maxLength = 99),
      "password" -> nonEmptyText(minLength = 4, maxLength = 99),
      "confirm" -> nonEmptyText(minLength = 4, maxLength = 99)
    ) verifying("Passwords do not match", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
        password.trim == confirmPassword.trim
     }
    })  verifying("Email address is not valid", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
         false
      //   RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    }) verifying("Username is not valid. A to Z and numbers only", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
         false
      //   RecipientController.ValidUsername.findFirstIn(username.trim).isDefined
      }
    }) verifying("Username is already taken", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
         false
      //   !Recipient.findByUsername(username.trim).isDefined
      }
    })
  )

	val loginForm = Form(
	   tuple(
	      "username" -> nonEmptyText(maxLength = 99),
	      "password" -> nonEmptyText(maxLength = 99),
      	"source" -> optional(text)
	    ) verifying("Log in failed. Username does not exist or password is invalid", fields => fields match {
       case (username, password, source) => false
      //  case (username, password, source) => Recipient.authenticate(username, password).isDefined
      }) verifying("Your email address not yet been verified", fields => fields match {
       case (username, password, source) => false
      //  case (username, password, source) => Recipient.isEmailVerifiedOrNotRequired(username, password).isDefined
      })
	)

  val contactForm = Form(
    tuple(
      "name" -> nonEmptyText(maxLength = 99),
      "email" -> nonEmptyText(maxLength = 99),
      "username" -> optional(text(maxLength = 99)),
      "subject" -> optional(text(maxLength = 200)),
      "message" -> nonEmptyText(maxLength = 2000)
    ) verifying("Email address is not valid", fields => fields match {
      case (name, email, username, subject, message) => {
         false
      //   RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    })
  )

	def index = Action { implicit request =>
    findCurrentRecipient match {
      case Some(recipient) => {
      //   val wishlists = Wishlist.findByRecipient(recipient)
        Ok
      //   Ok(views.html.indexrecipient(WishController.editWishlistForm,wishlists))
      }
      case None => Ok(views.html.indexanon())
    }
	}

  	def register = Action { implicit request =>
  		registerForm.bindFromRequest.fold(
        errors => {
          Logger.warn("Registration failed: " + errors)
          BadRequest(views.html.register(errors))
        },
   	   registeredForm => {
	      	Logger.info("New registration: " + registeredForm._1)

	      	val recipient = Recipient(None,registeredForm._1,registeredForm._2,registeredForm._3,Some(registeredForm._4)).save

          EmailAlerter.sendNewRegistrationAlert(recipient)

          if(Recipient.emailVerificationRequired){
            val verificationHash = recipient.findVerificationHash.getOrElse(recipient.generateVerificationHash)
            EmailNotifier.sendEmailVerificationEmail(recipient, verificationHash)
            Redirect(routes.Application.index()).withNewSession.flashing("messageSuccess"->
              """
                Welcome, you have successfully registered.<br/>
                Please click on the link in the email we just sent to you
              """)
          } else {
            Redirect(routes.Application.index()).withSession(
              "username" -> registeredForm._1).flashing("messageSuccess"-> "Welcome, you have successfully registered")
          }

      	}
      )
  }

   def redirectToRegisterForm = Action { implicit request =>
      simpleRegisterForm.bindFromRequest.fold(
         errors => {
        BadRequest(views.html.register(registerForm))
         },
         emailInForm => {
            emailInForm match {
               case None => Ok(views.html.register(registerForm))
               case Some(email) =>
                  Ok(views.html.register(
                     registerForm.fill( email, None, email, "", "") ) )
            }
         }
      )
   }


	def showRegisterForm = Action { implicit request =>
		Ok(views.html.register(registerForm))
	}

	def showLoginForm = Action { implicit request =>
		Ok(views.html.login(loginForm))
	}

	def login = Action { implicit request =>
		loginForm.bindFromRequest.fold(
			errors => {
				Logger.info("Log in failed:"+ errors)
				BadRequest
				// BadRequest(views.html.login(errors))
			},
			loggedInForm => {
				Logger.debug("Logging in: " + loggedInForm._1)
            Redirect(routes.Application.index()).withSession(
               "username" -> loggedInForm._1).flashing("message"->"You have logged in")
         }
		)
	}
    

   def about = Action { implicit request =>
      Ok(views.html.about())
   }

   def contact = Action { implicit request =>
      Ok(views.html.contact(contactForm))
   }

  def sendContact =  Action { implicit request =>
    contactForm.bindFromRequest.fold(
      errors => {
          Logger.warn("Contact failed: " + errors)
          BadRequest(views.html.contact(errors))
      },
      contactFields => {

      //   EmailAl erter.sendContactMessage(contactFields._1, contactFields._2, contactFields._3, contactFields._4, contactFields._5, findCurrentRecipient)

        Redirect(routes.Application.index()).flashing("message"->"Your message was sent")

      }
    )
  }

  /*


   def logout = Action {
      Redirect(routes.Application.index).withNewSession.flashing("message"->"You have been logged out")
   }

   */
}
