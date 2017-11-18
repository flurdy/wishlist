package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex
import models._
import notifiers._
import repositories._

trait EmailAddressChecks {

   val ValidEmailAddresses =
      List(
         """^[\+\-\._A-Za-z0-9]+@[-\.A-Za-z0-9]{2,}$""".r,  // expected characters
         """^[^+]+(\+[^+]+)?@.+$""".r,                      // can have 1 plus in local alias
         """^.+@[^\-].*[^\-]$""".r                          // domain does not end or start with -
      )

   val InvalidEmailAddress = """\.\.""".r                   // no double dotting

}

trait ContactForm {

   def ValidEmailAddresses: List[Regex]
   def InvalidEmailAddress: Regex

   val contactForm = Form(
      tuple(
         "name" -> nonEmptyText(maxLength = 99),
         "email" -> nonEmptyText(maxLength = 99),
         "username" -> optional(text(maxLength = 99)),
         "subject" -> optional(text(maxLength = 200)),
         "message" -> nonEmptyText(maxLength = 2000)
      ) verifying("Email address is not valid", fields => fields match {
         case (_, email, _, _, _) => {
            ValidEmailAddresses.filterNot( r => r.findFirstIn(email.trim).isDefined ).isEmpty &&
               InvalidEmailAddress.findFirstIn(email.trim).isEmpty
         }
      })
   )
}


class ContactController @Inject()(cc: ControllerComponents, val recipientLookup: RecipientLookup, val emailNotifier: EmailNotifier, val appConfig: ApplicationConfig, usernameAction: UsernameAction, maybeCurrentRecipientAction: MaybeCurrentRecipientAction)
(implicit val executionContext: ExecutionContext)
extends AbstractController(cc) with Secured with WithAnalytics with RegisterForm with ContactForm with WithLogging with EmailAddressChecks {

   def contact = (usernameAction andThen maybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.contact(contactForm))
   }

   def redirectToContact = Action { implicit request =>
      Redirect(routes.ContactController.contact())
   }

  def sendContact =  (usernameAction andThen maybeCurrentRecipientAction) { implicit request =>
    contactForm.bindFromRequest.fold(
      errors => {
          logger.warn("Contact failed: " + errors.errors.headOption.map( e => s"${e.key}: ${e.message}").getOrElse(""))
          BadRequest(views.html.contact(errors))
      },
      contactFields => {

         emailNotifier.sendContactEmail( contactFields._1, contactFields._2,
                                         contactFields._3, contactFields._4,
                                         contactFields._5, request.currentRecipient )

        Redirect(routes.Application.index()).flashing("message"->"Your message was sent")

      }
    )
  }
}
