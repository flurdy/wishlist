package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._

object Application extends Controller {


	val ValidEmailAddress = """^[0-9a-zA-Z]([+-_\.\w]*[0-9a-zA-Z])*@([0-9a-zA-Z][-\w]*[0-9a-zA-Z]\.)+[a-zA-Z]{2,9}$""".r

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
    }) verifying("Username is already taken", fields => fields match {
     case (username, fullname, email, password, confirmPassword) => {
        !Dreamer.findByUsername(username.trim).isDefined
     }
    }) verifying("Email address is not valid", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
      	ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    })
  )


  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def register = Action { implicit request =>
  	registerForm.bindFromRequest.fold(
        errors => {
          Logger.warn("Registration failed: " + errors)
          BadRequest(views.html.application.register(errors))
        },
   	   registeredForm => {
	      	Logger.info("New registration: " + registeredForm._1)

	      	val dreamer = Dreamer(None,registeredForm._1,registeredForm._2,registeredForm._3,Some(registeredForm._4))
	      		
	      	dreamer.save	
	      	
	      	// TODO: Send email confirmation

         	Redirect(routes.Application.index()).flashing("message"-> "Welcome, you have successfully registered")
      	}
      )
  }

  def redirectToRegisterForm = Action { implicit request =>
  	simpleRegisterForm.bindFromRequest.fold(
        errors => {
          BadRequest(views.html.application.register(registerForm))
        },
   	   emailInForm => {
   	   	emailInForm match {
   	   		case None => Ok(views.html.application.register(registerForm))
   	   		case Some(email) => {   	   	
		         	Ok(views.html.application.register(	
		         			registerForm.fill( email, None, email, "", "") ) )		
   	   		}
   	   	}
      	}
      )
  }

  def showRegisterForm = Action { implicit request =>
  	Ok(views.html.application.register(registerForm))
  }

	def showLoginForm = Action {
		Ok(views.html.application.login())
	}

  def login = TODO
    
   def about = TODO
   
   def contact = TODO

	def search = TODO
  
}