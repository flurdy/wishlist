package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import notifiers._
import java.math.BigInteger
import java.security.SecureRandom

object Application extends Controller with Secured{


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
        RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    }) verifying("Username is not valid. A to Z and numbers only", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
        RecipientController.ValidUsername.findFirstIn(username.trim).isDefined
      }
    }) verifying("Username is already taken", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
        !Recipient.findByUsername(username.trim).isDefined
      }
    })
  )

	val loginForm = Form(
	   tuple(
	      "username" -> nonEmptyText(maxLength = 99),
	      "password" -> nonEmptyText(maxLength = 99),
      	"source" -> optional(text)
	    ) verifying("Log in failed. Username does not exist or password is invalid", fields => fields match {
	      case (username, password, source) => Recipient.authenticate(username, password).isDefined
    	}) 
	)	


  val contactForm = Form(
    tuple(
      "name" -> nonEmptyText(maxLength = 99),
      "email" -> nonEmptyText(maxLength = 99),
      "username" -> optional(text(maxLength = 99)),
      "subject" -> optional(text(maxLength = 200)),
      "message" -> nonEmptyText(maxLength = 99),
      "validation" -> nonEmptyText(maxLength = 20)
    ) verifying("Email address is not valid", fields => fields match {
      case (name, email, username, subject, message, validation) => {
        RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    })
  )


	def index = Action { implicit request =>
    findCurrentRecipient match {
      case Some(recipient) => {
        val wishlists = Wishlist.findWishlistsByUsername(recipient.username)
        Ok(views.html.indexrecipient(WishController.editWishlistForm,wishlists))
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

	      	val recipient = Recipient(None,registeredForm._1,registeredForm._2,registeredForm._3,Some(registeredForm._4))
	      		
	      	recipient.save	
	      	
	      	// TODO: Send email verification
          EmailAlerter.sendNewRegistrationAlert(recipient)

         	Redirect(routes.Application.index()).withSession(
          "username" -> registeredForm._1).flashing("messageSuccess"-> "Welcome, you have successfully registered")
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
   	   		case Some(email) => {   	   	
		         	Ok(views.html.register(	
		         			registerForm.fill( email, None, email, "", "") ) )		
   	   		}
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
				BadRequest(views.html.login(errors))
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

  private def generateCaptchaId : String = {
    new BigInteger(130, new SecureRandom()).toString(16)
  }
   
  private def findCaptchaId : String = { implicit session : Session =>
    session.get("capchaId").getOrElse(generateCaptchaId)
  }

  private def isSameAsCaptcha(formId:String,captchaId:String) : Boolean = {
    false
  }
   
   def contact = Action { implicit request =>
    val captchaId = generateCaptchaId
    Ok(views.html.contact(contactForm,captchaId)).withSession(session + "captchaId"->captchaId)
  }
  
  def sendContact =  Action { implicit request =>
    contactForm.bindFromRequest.fold(
      errors => {
          Logger.warn("Registration failed: " + errors)
          val captchaId = generateCaptchaId
          BadRequest(views.html.contact(errors,captchaId)).withSession(session + "captchaId"->captchaId)
      },
      contactFields => {

        if( isSameAsCaptcha( contactFields._6.trim , findCaptchaId ) ){
          
          EmailAlerter.sendContactMessage(contactFields._1, contactFields._2, contactFields._3, contactFields._4, contactFields._5, findCurrentRecipient)

          Redirect(routes.Application.index()).flashing("message"->"Your message was sent")

        } else {
          val captchaId = generateCaptchaId
          BadRequest(views.html.contact(contactForm.fill(contactFields),captchaId)).flashing("messageError"->"Try another validation").withSession(session + "captchaId"->captchaId)
        }

      }
    )
  }


   def logout = Action {
      Redirect(routes.Application.index).withNewSession.flashing("message"->"You have been logged out")
   }
  
}











