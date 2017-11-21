package controllers.testonly

import javax.inject.Inject
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.{ExecutionContext, Future}
import play.api.http.HeaderNames
import controllers.{Secured, WithLogging}
import models._
import repositories._


class TestOnlyRecipientController @Inject()(cc: ControllerComponents,
      val recipientLookup: RecipientLookup, val recipientRepository: RecipientRepository, val appConfig: ApplicationConfig)
(implicit val executionContext: ExecutionContext)
extends AbstractController(cc) with Secured with WithAnalytics with WithLogging {

   def findVerification(username: String) = Action.async{ _ =>
      recipientLookup.findRecipient(username.toLowerCase().trim).flatMap {
         case Some(recipient) =>
            recipientRepository.findVerificationHash(recipient).map {
               case Some(hash) =>
                  logger.warn(s"TEST ONLY: Redirecting [$username] to hash value: $hash")
                  // Ok.withHeaders(HeaderNames.LOCATION -> s"/recipient/${username.toLowerCase.trim}/verify/$hash/")
                  Redirect(s"/recipient/${username.toLowerCase.trim}/verify/$hash/")
               case _ =>
                  logger.info(s"No verification has found for [$username]")
                  NotFound
            }
         case _ =>
            logger.info(s" No recipient found for [$username]")
            Future.successful(Conflict)
      }
   }

}
