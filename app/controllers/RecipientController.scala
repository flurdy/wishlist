package controllers

import javax.inject.{Inject, Singleton}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import models._
import play.api.http.HeaderNames
import repositories._
// import notifiers._
// import scravatar._


@Singleton
class RecipientController @Inject() (val configuration: Configuration, val recipientLookup: RecipientLookup)
         (implicit val recipientRepository: RecipientRepository, val wishlistRepository: WishlistRepository, val reservationRepository: ReservationRepository)
extends Controller with Secured with WithAnalytics with WishForm with WithLogging {

/*

object RecipientController {

  val ValidEmailAddress = """^[0-9a-zA-Z]([+-_\.\w]*[0-9a-zA-Z])*@([0-9a-zA-Z][-\w]*[0-9a-zA-Z]\.)+[a-zA-Z]{2,9}$""".r

  val ValidUsername= """^[0-9a-zA-Z_-]+$""".r


  val editRecipientForm = Form(
    tuple(
      "oldusername" -> nonEmptyText(maxLength = 99),
      "username" -> nonEmptyText(maxLength = 99),
      "fullname" -> optional(text(maxLength = 99)),
      "email" -> nonEmptyText(maxLength = 99)
    ) verifying("Email address is not valid", fields => fields match {
      case (oldusername, username, fullname, email) => {
        ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    })   verifying("Username is not valid. A to Z and numbers only. No spaces. Sorry", fields => fields match {
      case (oldusername, username, fullname, email) => {
        ValidUsername.findFirstIn(username.trim).isDefined
      }
    })  verifying("Username is already taken", fields => fields match {
      case (oldusername, username, fullname, email) => {
        oldusername == username || !Recipient.findByUsername(username.trim).isDefined
      }
    })
  )


  def changePasswordForm(username:String) = Form(
    tuple(
      "password" -> nonEmptyText(minLength = 4, maxLength = 99),
      "newpassword" -> nonEmptyText(minLength = 4, maxLength = 99),
      "confirm" -> nonEmptyText(minLength = 4, maxLength = 99)
    ) verifying("Passwords do not match", fields => fields match {
      case (password, newpassword, confirmPassword) => {
        newpassword.trim == confirmPassword.trim
      }
    }) verifying("Current password is invalid", fields => fields match {
      case (password, newpassword, confirmPassword) =>  Recipient.authenticate(username, password).isDefined
    })
  )


  def gravatarUrl(recipient:Recipient) = Gravatar(recipient.email).default(Monster).maxRatedAs(PG).size(100).avatarUrl

}

class RecipientController extends Controller with Secured {
  import RecipientController._


  val emailVerificationForm = Form(
    tuple(
      "username" -> nonEmptyText(maxLength = 99),
      "email" -> nonEmptyText(maxLength = 99),
      "password" -> nonEmptyText(minLength = 4, maxLength = 99)
    ) verifying("Email address is not valid", fields => fields match {
      case (username, email, password) => {
        ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    }) verifying("Authentication failed or username and email does not match", fields => fields match {
      case (username, email, password) =>  Recipient.findByUsernameAndEmail(username,email).isDefined && Recipient.authenticate(username, password).isDefined
    })
  )
*/

  val resetPasswordForm = Form(
    tuple(
    "username" -> nonEmptyText(maxLength = 99),
    "email" -> nonEmptyText(maxLength = 99)
    ) verifying("Email address is not valid", fields => fields match {
      case (username, email) => {
         false
      //   RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    }) verifying("Username is not valid. A to Z and numbers only", fields => fields match {
      case (username, email) => {
         false
      //   RecipientController.ValidUsername.findFirstIn(username.trim).isDefined
      }
    }) verifying("Username and email does match or exist", fields => fields match {
      case (username, email) => {
         false
      //   if(RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined &&
            // RecipientController.ValidUsername.findFirstIn(username.trim).isDefined) {
         //  Recipient.findByUsernameAndEmail(username.trim,email.trim).isDefined
      //   } else {
         //  true
      //   }
      }
    })
  )


   def showProfile(username: String) = (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>
      recipientLookup.findRecipient(username) flatMap {
         case Some(recipient) if request.currentRecipient.exists( r => recipient.isSameUsername(r)) =>
            for {
               wishlists    <- recipient.findWishlists
               organised    <- recipient.findOrganisedWishlists
               reservations <- recipient.findAndInflateReservations
               gravatarUrl  =  None
            } yield Ok(views.html.recipient.profile(recipient, wishlists,
                  organised, reservations, editWishlistForm, gravatarUrl ))
         case Some(recipient) =>
            for {
             wishlists    <- recipient.findWishlists
             gravatarUrl  =  None
            } yield Ok(views.html.recipient.profile(recipient, wishlists,
                  organisedWishlists = Nil, reservations = Nil, editWishlistForm, gravatarUrl ))
         case _ => Future.successful( NotFound ) // TODO
      }
   }

   def showEditRecipient(username:String) = TODO

   /*

  def showEditRecipient(username:String) = isProfileRecipient(username) { (profileRecipient) => implicit request =>
    val editForm = editRecipientForm.fill(profileRecipient.username,profileRecipient.username,profileRecipient.fullname,profileRecipient.email)
    Ok(views.html.recipient.editrecipient(profileRecipient,editForm))
  }


  def showDeleteRecipient(username:String) = isProfileRecipient(username)  { (profileRecipient) => implicit request =>
    Ok(views.html.recipient.deleterecipient(profileRecipient))
  }


  def deleteRecipient(username:String) = isProfileRecipient(username)  { (profileRecipient) => implicit request =>

    profileRecipient.delete

    Redirect(routes.Application.index()).flashing("messageWarning" -> "Recipient deleted")
  }


  def updateRecipient(username:String)  = isProfileRecipient(username)  { (profileRecipient) => implicit request =>
    editRecipientForm.bindFromRequest.fold(
      errors => {
        Logger.warn("Update failed: " + errors)
        BadRequest(views.html.recipient.editrecipient(profileRecipient,errors))
      },
      editForm => {
        val updatedRecipient = profileRecipient.copy(
          fullname=editForm._3,
          email=editForm._4 )

        updatedRecipient.update

        Redirect(routes.RecipientController.showEditRecipient(username)).flashing("message" -> "Recipient updated")
      }
    )
  }

*/

   def showResetPassword = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.recipient.passwordreset(resetPasswordForm))
   }

/*

  def resetPassword = Action { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      errors => {
        BadRequest(views.html.recipient.passwordreset(errors))
      },
      resetForm => {
        Recipient.findByUsernameAndEmail(resetForm._1,resetForm._2) match {
          case Some(recipient) => {
            Logger.info("Password reset requested for: " + recipient.recipientId)

            val newPassword = recipient.resetPassword

            EmailNotifier.sendPasswordResetEmail(recipient,newPassword)

            Redirect(routes.Application.index()).flashing("messageWarning" -> "Password reset and sent by email")
          }
          case None => {
            NotFound(views.html.error.notfound())
          }
        }
      }
    )
  }



   def showChangePassword(username:String)  = isProfileRecipient(username) { (profileRecipient) => implicit request =>
    Ok(views.html.recipient.passwordchange(changePasswordForm(username)))
  }


  def updatePassword(username:String)  = isProfileRecipient(username) { (profileRecipient) => implicit request =>
    changePasswordForm(username).bindFromRequest.fold(
      errors => {
        BadRequest(views.html.recipient.passwordchange(errors))
      },
      resetForm => {

        Logger.info("Password change requested for: " + profileRecipient.username)

        profileRecipient.updatePassword(resetForm._3)

        EmailNotifier.sendPasswordChangeEmail(profileRecipient)

        Redirect(routes.Application.showLoginForm)
            .withNewSession.flashing("messageWarning" -> "Password changed successfully. Please log in again")
      }
    )
  }

*/

   def verifyEmail(username: String, verificationHash: String) = Action.async { implicit request =>

      def redirectToLogin: Result = Redirect(routes.LoginController.showLoginForm)
               .withNewSession.flashing("messageSuccess" -> "Email address verified. Please log in")

      logger.info(s"Verifying email for $username")
      recipientLookup.findRecipient(username) flatMap {
         case Some(recipient) =>
            recipient.isVerified.flatMap {
               case true =>
                  logger.warn(s"Already verified for $username")
                  Future.successful( redirectToLogin )
               case false =>
                  recipient.doesVerificationMatch(verificationHash).flatMap {
                     case true =>
                        logger.debug(s"Verification match for $username")
                        recipient.setEmailAsVerified(verificationHash).map {
                           case true  => redirectToLogin
                           case false => throw new IllegalStateException(s"Unable to set $username as verified")
                        }
                     case false =>
                        logger.warn(s"Verification for $username does not match [$verificationHash]")
                        Future.successful( BadRequest )
                  }
            }
         case _ => Future.successful( NotFound )
      }
   }

/*

  def verifyEmail(username:String,verificationHash:String) = Action { implicit request =>
    Recipient.findByUsername(username) match {
      case Some(recipient) => {
        Logger.info("Verifying email for %s".format(username))
        if(recipient.isEmailVerified) {
          Logger.warn("Already verified for %s".format(username))
          Redirect(routes.Application.showLoginForm)
            .withNewSession.flashing("message" -> "Email address verified. Please log in")
        } else {
          if(recipient.doesVerificationMatch(verificationHash)) {
            recipient.setEmailAsVerified
            Redirect(routes.Application.showLoginForm)
              .withNewSession.flashing("messageSuccess" -> "Email address verified. Please log in")
          } else {
            Logger.warn("Verifying does not match for %s".format(username))
            NotFound(views.html.error.notfound())
          }
        }
      }
      case None => NotFound(views.html.error.notfound())
    }
  }

*/

   def showResendVerification = TODO

/*

  def showResendVerification = Action { implicit request =>
    Ok(views.html.recipient.emailverification(emailVerificationForm))
  }


  def resendVerification = Action { implicit request =>
    emailVerificationForm.bindFromRequest.fold(
      errors => {
        BadRequest(views.html.recipient.emailverification(errors))
      },
      verificationForm => {
        Recipient.findByUsernameAndEmail(verificationForm._1,verificationForm._2) match {
          case Some(recipient) => {
            Logger.info("Verification resend requested for: " + recipient.recipientId)

            if (recipient.isEmailVerified){

              Redirect(routes.Application.login()).flashing("messageWarning" -> "Email already verified")
            } else {

              val verificationHash = recipient.generateVerificationHash
              EmailNotifier.sendEmailVerificationEmail(recipient, verificationHash)

              Redirect(routes.Application.index()).flashing("messageWarning" -> "Verification resent by email")
            }

          }
          case None => {
            NotFound(views.html.error.notfound())
          }
        }
      }
    )
  }
  */
}

