package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex
import models._
import repositories._
import notifiers._


trait RegisterForm {

   def ValidEmailAddresses: List[Regex]
   def InvalidEmailAddress: Regex

   def isValidEmailAddress(email: String) =
      ValidEmailAddresses.filterNot( r => r.findFirstIn(email.trim).isDefined ).isEmpty &&
         InvalidEmailAddress.findFirstIn(email.trim).isEmpty

   def isValidUsername(username: String) = ValidUsername.findFirstIn(username.trim).isDefined

   val ValidUsername = """^[a-zA-Z0-9\-_]{3,99}$""".r

	val simpleRegisterForm = Form {
		"email" -> optional(text(maxLength = 99))
 	}

 	val registerForm = Form(
    tuple(
      "username" -> nonEmptyText(minLength = 3, maxLength = 99),
      "fullname" -> optional(text(maxLength = 99)),
      "email" -> nonEmptyText(maxLength = 99),
      "password" -> nonEmptyText(minLength = 4, maxLength = 99),
      "confirm" -> nonEmptyText(minLength = 4, maxLength = 99)
    ) verifying("Registration failed. Passwords do not match", fields => fields match {
      case (_, _, _, password, confirmPassword) =>
        password.trim == confirmPassword.trim
    })  verifying("Registration failed. Email address is not valid", fields => fields match {
      case (_, _, email, _, _) =>
         isValidEmailAddress(email)
    }) verifying("Registration failed. Username is not valid. A to Z and numbers only", fields => fields match {
      case (username, _, _, _, _) =>
         isValidUsername(username)
    })
  )
}

class RegisterController @Inject()(cc: ControllerComponents, val recipientFactory: RecipientFactory, 
   val recipientLookup: RecipientLookup, val emailNotifier: EmailNotifier, 
   val usernameValidator: UsernameValidator, val appConfig: ApplicationConfig,
   usernameAction: UsernameAction, maybeCurrentRecipientAction: MaybeCurrentRecipientAction)
(implicit val executionContext: ExecutionContext, val recipientRepository: RecipientRepository, val featureToggles: FeatureToggles)
extends AbstractController(cc) with Secured with WithAnalytics with RegisterForm with EmailAddressChecks with WithLogging {

  	def register = (usernameAction andThen maybeCurrentRecipientAction).async { implicit request =>

      def badRequest(form: Form[(String, Option[String], String, String, String)], message: Option[String]) =
        Future.successful( BadRequest(views.html.register( form, message) ) )

  		registerForm.bindFromRequest.fold(
        errors => {
          logger.warn("Registration failed: " + errors.errors.headOption.map( e => s"${e.key}: ${e.message}").getOrElse(""))
          badRequest(errors, None)
        },
   	   registeredForm => {

            def fillForm = registerForm.fill(
               (registeredForm._1, registeredForm._2, registeredForm._3, "", ""))

            recipientLookup.findRecipient(registeredForm._1.trim.toLowerCase()) flatMap {
               case None =>

                  usernameValidator.isValid(registeredForm._1.trim.toLowerCase()) flatMap {
                     case true =>
                        recipientFactory.newRecipient( registeredForm ).save.flatMap {
                           recipient =>
                              logger.info("New registration: " + registeredForm._1)
                              emailNotifier.sendNewRegistrationAlert(recipient)

                              if(FeatureToggle.EmailVerification.isEnabled()){
                                 recipient.findOrGenerateVerificationHash.flatMap { verificationHash =>
                                    val verificationRoute = s"/recipient/${recipient.username}/verify/$verificationHash/"
                                    emailNotifier.sendEmailVerification(recipient, verificationRoute).map { _ =>
                                       Redirect(routes.Application.index()).withNewSession.flashing("messageSuccess"->
                                          """Welcome, you have successfully registered.<br/>
                                          Please click on the verification link in the email we just sent to you""")
                                    }
                                 }
                              } else {
                                 logger.info("Email verification not enabled")
                                 Future.successful(
                                    Redirect(routes.Application.index()).withSession(
                                       "username" -> registeredForm._1).flashing(
                                          "messageSuccess"-> "Welcome, you have successfully registered"))
                              }
                        }
                     case false =>
                        logger.info(s"Username invalid: [$registeredForm._1]")
                        badRequest( fillForm, Some("Registration failed. Username unavailable") )
                  }
               case _ =>
                  logger.info(s"Username taken: [$registeredForm._1]")
                  badRequest( fillForm, Some("Registration failed. Username already registered") )
            }
      	}
      )
  }

   def redirectToRegisterForm = (usernameAction andThen maybeCurrentRecipientAction) { implicit request =>
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

   def showRegisterForm = (usernameAction andThen maybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.register(registerForm))
   }
}
