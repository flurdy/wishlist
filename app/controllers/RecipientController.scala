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

trait RecipientForm extends RegisterForm {

  val editRecipientForm = Form(
    tuple(
      "oldusername" -> nonEmptyText(maxLength = 99),
      "username" -> nonEmptyText(maxLength = 99),
      "fullname" -> optional(text(maxLength = 99)),
      "email" -> nonEmptyText(maxLength = 99)
    ) verifying("Email address is not valid", fields => fields match {
      case (_, _, _, email) => {
        ValidEmailAddresses.filterNot( r => r.findFirstIn(email.trim).isDefined ).isEmpty &&
            InvalidEmailAddress.findFirstIn(email.trim).isEmpty
      }
    }) verifying("Username is not valid. A to Z and numbers only. No spaces. Sorry", fields => fields match {
      case (_, username, _, _) => {
        ValidUsername.findFirstIn(username.trim).isDefined
      }
    // }) verifying("Username is already taken", fields => fields match {
    //   case (oldusername, username, fullname, email) => {
    //     oldusername == username || !Recipient.findByUsername(username.trim).isDefined
    //   }
    })
  )

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

   val changePasswordForm = Form(
      tuple(
         "password" -> nonEmptyText(minLength = 4, maxLength = 99),
         "newpassword" -> nonEmptyText(minLength = 4, maxLength = 99),
         "confirm" -> nonEmptyText(minLength = 4, maxLength = 99)
      ) verifying("Passwords do not match", fields => fields match {
         case (_, newpassword, confirmPassword) => {
            newpassword.trim == confirmPassword.trim
         }
      // }) verifying("Current password is invalid", fields => fields match {
      //    case (password, newpassword, confirmPassword) =>  Recipient.authenticate(username, password).isDefined
      })
   )

}

@Singleton
class RecipientController @Inject() (val configuration: Configuration, val recipientLookup: RecipientLookup)
         (implicit val recipientRepository: RecipientRepository, val wishlistRepository: WishlistRepository,
         val reservationRepository: ReservationRepository)
extends Controller with Secured with WithAnalytics with WishlistForm with RecipientForm with EmailAddressChecks with WithLogging {

/*

object RecipientController {

  val ValidEmailAddress = """^[0-9a-zA-Z]([+-_\.\w]*[0-9a-zA-Z])*@([0-9a-zA-Z][-\w]*[0-9a-zA-Z]\.)+[a-zA-Z]{2,9}$""".r

  val ValidUsername= """^[0-9a-zA-Z_-]+$""".r



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

   def showProfile(username: String) = (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>
      recipientLookup.findRecipient(username) flatMap {
         case Some(recipient) if request.currentRecipient.exists( r => recipient.isSameUsername(r)) =>
            for {
               wishlists    <- recipient.findAndInflateWishlists
               organised    <- recipient.findAndInflateOrganisedWishlists
               reservations <- recipient.findAndInflateReservations
               gravatarUrl  =  None
            } yield Ok(views.html.recipient.profile(recipient, wishlists,
                  organised, reservations, editWishlistForm, gravatarUrl ))
         case Some(recipient) =>
            for {
             wishlists    <- recipient.findAndInflateWishlists
             gravatarUrl  =  None
            } yield Ok(views.html.recipient.profile(recipient, wishlists,
                  organisedWishlists = Nil, reservations = Nil, editWishlistForm, gravatarUrl ))
         case _ => Future.successful( NotFound ) // TODO
      }
   }

   def redirectToShowEditRecipient(username: String) = Action {
      Redirect(routes.RecipientController.showEditRecipient(username))
   }

   def showEditRecipient(username: String) = (UsernameAction andThen CurrentRecipientAction).async { implicit request =>

      recipientLookup.findRecipient(username) map {
         case Some(recipient) if request.currentRecipient.exists( r => recipient.isSameUsername(r)) =>
           val editForm = editRecipientForm.fill(
             username, username, recipient.fullname, recipient.email)
           Ok( views.html.recipient.editrecipient(recipient, editForm) )
         case Some(recipient) => Unauthorized // TODO
         case _ => NotFound // TODO
      }
   }

   def showDeleteRecipient(username: String) = TODO

   /*

   def showDeleteRecipient(username:String) = isProfileRecipient(username)  { (profileRecipient) => implicit request =>
     Ok(views.html.recipient.deleterecipient(profileRecipient))
   }

   */

  def alsoDeleteRecipient(username: String) = deleteRecipient(username)

  def deleteRecipient(username: String) = TODO

  /*

  def deleteRecipient(username:String) = isProfileRecipient(username)  { (profileRecipient) => implicit request =>

    profileRecipient.delete

    Redirect(routes.Application.index()).flashing("messageWarning" -> "Recipient deleted")
  }

  */

   def alsoUpdateRecipient(username: String) = updateRecipient(username)

   def updateRecipient(username: String) = (UsernameAction andThen CurrentRecipientAction).async { implicit request =>

      recipientLookup.findRecipient(username) flatMap {
         case Some(recipient) if request.currentRecipient.exists( r => recipient.isSameUsername(r)) =>

            def badRequest(form: Form[(String,String,Option[String],String)], errorMessage: Option[String]) =
                Future.successful(
                   BadRequest(views.html.recipient.editrecipient(
                      recipient, form, errorMessage )))

            editRecipientForm.bindFromRequest.fold(
               errors => {
                  logger.warn("Update failed: " + errors)
                  badRequest(errors, None)
               }, {
                  case ( oldUsername, newUsername, newFullname, newEmail) =>

                     def editForm = editRecipientForm.fill( (oldUsername, newUsername, newFullname, newEmail) )

                     if(username == oldUsername){
                        recipient.copy(
                          fullname = newFullname,
                          email    = newEmail
                        ).update.map { _ =>
                          Redirect(routes.RecipientController.showProfile(username))
                              .flashing("messageSuccess" -> "Recipient updated")
                        }
                    } else {
                       logger.warn(s"Old username mismatch for [$username] and [$oldUsername]")
                       badRequest(editForm, None)
                    }
               }
            )
          case Some(recipient) =>
             logger.warn(s"Unauthorized to update recipient [$username] as [${request.currentRecipient}]")
             Future.successful( Unauthorized ) // TODO
          case _ => Future.successful( NotFound ) // TODO
      }
   }

   def showResetPassword = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.recipient.passwordreset(resetPasswordForm))
   }

   def resetPassword = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>

      Conflict // TODO

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

*/

   def showChangePassword(username: String) = (UsernameAction andThen CurrentRecipientAction) { implicit request =>
      request.currentRecipient match {
         case Some(recipient) if recipient.username == username =>
            Ok(views.html.recipient.passwordchange(changePasswordForm))
         case Some(recipient) =>
            Unauthorized // TODO
         case _ =>
            NotFound // TODO
      }
   }

   def updatePassword(username: String) = (UsernameAction andThen CurrentRecipientAction).async { implicit request =>

      changePasswordForm.bindFromRequest.fold(
         errors => {
            Future.successful( BadRequest(views.html.recipient.passwordchange(errors) ))
         },{
            case (oldPassword, newPassword, confirmPassword) =>
               request.currentRecipient match {
                  case Some(recipient) if recipient.username == username =>
                     recipient.authenticate(oldPassword.trim) flatMap {
                        case true =>
                           logger.info(s"Changing password for $username")

                           recipient.updatePassword( newPassword ) map { _ =>

                              //   EmailNotifier.sendPasswordChangeEmail(profileRecipient) // TODO

                              Redirect(routes.LoginController.showLoginForm)
                                  .withNewSession.flashing("messageWarning" -> "Password changed successfully. Please log in again")
                           }
                        case false =>
                          Future.successful( BadRequest(
                             views.html.recipient.passwordchange(changePasswordForm,
                                Some("Authentication failed. Check existing password"))) )
                     }
                  case Some(recipient) =>
                     logger.warn(s"Unauthorized to update password for recipient [$username] as [${request.currentRecipient}]")
                     Future.successful( Unauthorized(
                        views.html.recipient.passwordchange(changePasswordForm,
                           Some("Unauthorized to update password for recipient [$username]") )) )
                  case _ =>
                     Future.successful( NotFound ) // TODO
               }
         }
      )
   }

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

   def resendVerification = TODO

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
