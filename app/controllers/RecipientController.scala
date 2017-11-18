package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Results.{NotFound, Unauthorized}
import scala.concurrent.{ExecutionContext, Future}
import com.flurdy.sander.primitives._
import models._
import repositories._
import notifiers._
import scravatar._


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
      case (_, username, _, _) =>
        ValidUsername.findFirstIn(username.trim).isDefined
    })
  )

  val resetPasswordForm = Form(
    tuple(
    "username" -> nonEmptyText(maxLength = 99),
    "email" -> nonEmptyText(maxLength = 99)
    ) verifying("Email address is not valid", fields => fields match {
      case (_, email) =>
         isValidEmailAddress(email)
    }) verifying("Username is not valid. A to Z and numbers only", fields => fields match {
      case (username, _) =>
         isValidUsername(username)
    })
  )

   val changePasswordForm = Form(
      tuple(
         "password" -> nonEmptyText(minLength = 4, maxLength = 99),
         "newpassword" -> nonEmptyText(minLength = 4, maxLength = 99),
         "confirm" -> nonEmptyText(minLength = 4, maxLength = 99)
      ) verifying("Passwords do not match", fields => fields match {
         case (_, newpassword, confirmPassword) =>
            newpassword.trim == confirmPassword.trim
      })
   )


   val emailVerificationForm = Form(
      tuple(
         "username" -> nonEmptyText(maxLength = 99),
         "email" -> nonEmptyText(maxLength = 99),
         "password" -> nonEmptyText(minLength = 4, maxLength = 99)
      ) verifying("Email address is not valid", fields => fields match {
         case (_, email, _) =>
            isValidEmailAddress(email)
      })
   )

}


trait RecipientActions {

   implicit def analyticsDetails: Option[AnalyticsDetails]

   def onlyRecipientEditorAction(username: String, ec: ExecutionContext)(implicit recipientLookup: RecipientLookup) = new ActionFilter[UsernameRequest] {
      implicit val executionContext: ExecutionContext = ec
      def filter[A](input: UsernameRequest[A]) = {
         implicit val flash = input.flash
         input.username.fold[Future[Option[Result]]]{
            implicit val currentRecipient = None
            Future.successful( Some( NotFound(views.html.error.notfound()) ) )
         }{ sessionUsername =>
            recipientLookup.findRecipient(username) flatMap {
               case Some(pR) =>
                  recipientLookup.findRecipient(sessionUsername) map {
                     case Some(sR) if sR.isSame(pR) => None
                     case maybeSr =>
                        implicit val currentRecipient = maybeSr
                        Some(Unauthorized(views.html.error.permissiondenied()))
                  }
               case _ =>
                  implicit val currentRecipient = None
                  Future.successful( Some(NotFound(views.html.error.notfound())) )
            }
         }
      }
   }

   def isLoggedIn(ec: ExecutionContext)(implicit recipientLookup: RecipientLookup) = new ActionFilter[UsernameRequest] {
      implicit val executionContext: ExecutionContext = ec
      def filter[A](input: UsernameRequest[A]) = {
         implicit val flash = input.flash
         input.username.fold[Future[Option[Recipient]]]{
            Future.successful( None )
         }{ sessionUsername =>
            recipientLookup.findRecipient(sessionUsername)
         }.map {
            case Some(_) => None
            case _ =>
               implicit val currentRecipient = None
               Some(Unauthorized(views.html.error.permissiondenied()))
         }
      }
   }
}


trait WithGravatarUrl {

   def generateGravatarUrl(recipient: Recipient)(implicit featureToggles: FeatureToggles) =
      FeatureToggle.Gravatar.isEnabled().some.map { _ =>
         Gravatar(recipient.email).default(Monster).maxRatedAs(PG).size(100).avatarUrl
      }
}


class RecipientController @Inject()(
            cc: ControllerComponents,
            val emailNotifier: EmailNotifier,
            val appConfig: ApplicationConfig,
            usernameAction: UsernameAction,
            maybeCurrentRecipientAction: MaybeCurrentRecipientAction)
         (implicit val executionContext: ExecutionContext,
            val recipientRepository: RecipientRepository,
            val recipientLookup: RecipientLookup,
            val wishlistRepository: WishlistRepository,
            val wishLinkRepository: WishLinkRepository,
            val wishLookup: WishLookup,
            val wishRepository: WishRepository,
            val wishEntryRepository: WishEntryRepository,
            val wishlistOrganiserRepository: WishlistOrganiserRepository,
            val reservationRepository: ReservationRepository,
            val featureToggles: FeatureToggles)
extends AbstractController(cc) with Secured with RecipientActions with WithAnalytics with WishlistForm
         with RecipientForm with EmailAddressChecks with WithLogging with WithGravatarUrl {


   def showProfile(username: String) = (usernameAction andThen maybeCurrentRecipientAction).async { implicit request =>
      recipientLookup.findRecipient(username) flatMap {
         case Some(recipient) if request.currentRecipient.exists( r => recipient.isSameUsername(r)) =>
            for {
               wishlists    <- recipient.findAndInflateWishlists
               organised    <- recipient.findAndInflateOrganisedWishlists
               reservations <- recipient.findAndInflateReservations
               gravatarUrl  =  generateGravatarUrl(recipient)
            } yield Ok(views.html.recipient.profile(recipient, wishlists,
                  organised, reservations, editWishlistForm, gravatarUrl ))
         case Some(recipient) =>
            for {
             wishlists    <- recipient.findAndInflateWishlists
             gravatarUrl  =  generateGravatarUrl(recipient)
            } yield Ok(views.html.recipient.profile(recipient, wishlists,
                  organisedWishlists = Nil, reservations = Nil, editWishlistForm, gravatarUrl ))
         case _ => Future.successful( NotFound(views.html.error.notfound()) )
      }
   }

   def redirectToShowProfile(username: String) = Action {
      Redirect(routes.RecipientController.showProfile(username))
   }

   def redirectToShowEditRecipient(username: String) = Action {
      Redirect(routes.RecipientController.showEditRecipient(username))
   }

   def showEditRecipient(username: String) = (usernameAction andThen onlyRecipientEditorAction(username, executionContext)
                        andThen maybeCurrentRecipientAction).async { implicit request =>
      recipientLookup.findRecipient(username) map {
         case Some(recipient) if request.currentRecipient.exists( r => recipient.isSameUsername(r)) =>
           val editForm = editRecipientForm.fill(
             username, username, recipient.fullname, recipient.email)
           Ok( views.html.recipient.editrecipient(recipient, editForm) )
         case Some(recipient) => Unauthorized(views.html.error.permissiondenied())
         case _ => NotFound(views.html.error.notfound())
      }
   }

   def showDeleteRecipient(username: String) = (usernameAction andThen onlyRecipientEditorAction(username, executionContext)
                        andThen maybeCurrentRecipientAction).async { implicit request =>
      recipientLookup.findRecipient(username) map {
         case Some(recipient) if request.currentRecipient.exists( r => recipient.isSameUsername(r)) =>
            Ok(views.html.recipient.deleterecipient(recipient))
          case Some(recipient) => Unauthorized(views.html.error.permissiondenied())
          case _ => NotFound(views.html.error.notfound())
      }
   }
   def alsoDeleteRecipient(username: String) = deleteRecipient(username)

   def deleteRecipient(username: String) = (usernameAction andThen onlyRecipientEditorAction(username, executionContext)
                        andThen maybeCurrentRecipientAction).async { implicit request =>
      recipientLookup.findRecipient(username) flatMap {
         case Some(recipient) if request.currentRecipient.exists( r => recipient.isSameUsername(r)) =>
            logger.info("Deleting recipient: "+ recipient.username)
            emailNotifier.sendRecipientDeletedAlert(recipient)
            emailNotifier.sendRecipientDeletedNotification(recipient)
            recipient.delete.map { _ =>
               Redirect(routes.Application.index()).withNewSession.flashing("messageWarning" -> "Recipient deleted")
            }
         case Some(recipient) => Future.successful( Unauthorized(views.html.error.permissiondenied()) )
         case _ => Future.successful( NotFound(views.html.error.notfound()) )
      }
   }

   def alsoUpdateRecipient(username: String) = updateRecipient(username)

   def updateRecipient(username: String) = (usernameAction andThen onlyRecipientEditorAction(username, executionContext)
                        andThen maybeCurrentRecipientAction).async { implicit request =>

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
             Future.successful( Unauthorized(views.html.error.permissiondenied()) )
          case _ => Future.successful( NotFound(views.html.error.notfound()) )
      }
   }

   def showResetPassword = (usernameAction andThen maybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.recipient.passwordreset(resetPasswordForm))
   }

   def resetPassword = (usernameAction andThen maybeCurrentRecipientAction).async { implicit request =>

      resetPasswordForm.bindFromRequest.fold(
         errors => {
            Future.successful( BadRequest(views.html.recipient.passwordreset(errors)) )
         },{
            case (username, email) =>
               logger.info(s"Requesting password reset for $username")
               recipientLookup.findRecipient(username.trim) flatMap {
                  case Some(recipient) if recipient.email.toLowerCase == email.trim.toLowerCase =>

                     recipient.resetPassword() flatMap { case (_, newPassword) =>
                        emailNotifier.sendPasswordResetEmail(recipient, newPassword).map { _ =>
                           Redirect(routes.Application.index()).flashing("messageWarning" -> "Password reset information sent by email")
                        }
                     }
                  case _ => Future.successful( NotFound(views.html.error.notfound()) )
               }
         }
      )
   }

   def showChangePassword(username: String) = (usernameAction andThen onlyRecipientEditorAction(username, executionContext)
                        andThen maybeCurrentRecipientAction) { implicit request =>
      request.currentRecipient match {
         case Some(recipient) if recipient.username == username =>
            Ok(views.html.recipient.passwordchange(changePasswordForm))
         case Some(recipient) => Unauthorized(views.html.error.permissiondenied())
         case _ => NotFound(views.html.error.notfound())
      }
   }

   def updatePassword(username: String) = (usernameAction andThen onlyRecipientEditorAction(username, executionContext)
                        andThen maybeCurrentRecipientAction).async { implicit request =>

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

                           recipient.updatePassword( newPassword ) flatMap { _ =>
                              emailNotifier.sendPasswordChangedNotification(recipient) map { _ =>
                                 Redirect(routes.LoginController.showLoginForm)
                                    .withNewSession
                                    .flashing("messageWarning" ->
                                       "Password changed successfully. Please log in again")
                              }
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
                     Future.successful( NotFound(views.html.error.notfound()) )
               }
         }
      )
   }

   def verifyEmail(username: String, verificationHash: String) = (usernameAction andThen maybeCurrentRecipientAction).async { implicit request =>

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
                        Future.successful( Redirect(routes.RecipientController.showResendVerification())
                                 .flashing("messageError" -> "Verification error") )
                  }
            }
         case _ => Future.successful( NotFound(views.html.error.notfound()) )
      }
   }

   def showResendVerification = (usernameAction andThen maybeCurrentRecipientAction) { implicit request =>
     Ok(views.html.recipient.emailverification(emailVerificationForm))
   }

   def resendVerification = (usernameAction andThen maybeCurrentRecipientAction).async { implicit request =>
      emailVerificationForm.bindFromRequest.fold(
         errors => {
            Future.successful( BadRequest(views.html.recipient.emailverification(errors)) )
         },{
            case (username, email, password) =>
               recipientLookup.findRecipient(username.trim) flatMap {
                  case Some(recipient) if recipient.email.toLowerCase == email.trim.toLowerCase =>
                     recipient.authenticate(password) flatMap {
                        case true  =>
                           if(FeatureToggle.EmailVerification.isEnabled()){
                              recipient.isVerified flatMap {
                                 case false =>
                                    recipient.findOrGenerateVerificationHash.flatMap { verificationHash =>
                                       emailNotifier.sendEmailVerification(recipient, verificationHash).map { _ =>
                                            Redirect(routes.Application.index())
                                               .flashing("message" -> "Verification resent by email")
                                       }
                                    }
                                 case true  =>
                                    Future.successful(
                                       Redirect(routes.LoginController.login())
                                          .flashing("message" -> "Email already verified"))
                              }
                           } else
                              Future.successful(
                                 Redirect(routes.Application.index())
                                    .flashing("message" -> "Verification is not needed"))
                        case false =>
                           Future.successful( Unauthorized(views.html.error.permissiondenied()) )
                     }
                  case _ =>
                     Future.successful( NotFound(views.html.error.notfound()) )
               }
         }
      )
   }
}
