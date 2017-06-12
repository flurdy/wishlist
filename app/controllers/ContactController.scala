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
import notifiers._
// import java.math.BigInteger
// import java.security.SecureRandom
import scala.concurrent.Future
import scala.util.matching.Regex

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

@Singleton
class ContactController @Inject() (val configuration: Configuration, val recipientLookup: RecipientLookup, val emailNotifier: EmailNotifier)
extends Controller with Secured with WithAnalytics with RegisterForm with ContactForm with EmailAddressChecks {

   def contact = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.contact(contactForm))
   }

   def redirectToContact = Action { implicit request =>
      Redirect(routes.ContactController.contact())
   }

  def sendContact =  (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
    contactForm.bindFromRequest.fold(
      errors => {
          Logger.warn("Contact failed: " + errors.errors.headOption.map( e => s"${e.key}: ${e.message}").getOrElse(""))
          BadRequest(views.html.contact(errors))
      },
      contactFields => {

         emailNotifier.sendContactEmail( contactFields._1, contactFields._2,
                                         contactFields._3, contactFields._4, contactFields._5, request.currentRecipient )

      //   EmailAl erter.sendContactMessage(contactFields._1, contactFields._2, contactFields._3, contactFields._4, contactFields._5, findCurrentRecipient)

        Redirect(routes.Application.index()).flashing("message"->"Your message was sent")

      }
    )
  }

}