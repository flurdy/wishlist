package controllers.testonly

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
import controllers.{Secured, WithAnalytics}


@Singleton
class TestOnlyRecipientController @Inject() (val configuration: Configuration,
      val recipientLookup: RecipientLookup, val recipientRepository: RecipientRepository)
      extends Controller with Secured with WithAnalytics {

   def findVerification(username: String) = Action.async{ _ =>
      recipientLookup.findRecipient(username.toLowerCase().trim).flatMap {
         case Some(recipient) =>
            recipientRepository.findVerificationHash(recipient).map {
               case Some(hash) =>
                  Ok.withHeaders(HeaderNames.LOCATION -> s"/recipient/${username.toLowerCase.trim}/verify/$hash/")
               case _ => NotFound
            }
         case _ => Future.successful(NotFound)
      }
   }

}
