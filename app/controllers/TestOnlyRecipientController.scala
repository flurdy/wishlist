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
import controllers.{Secured, WithAnalytics, WithLogging}


@Singleton
class TestOnlyRecipientController @Inject() (val configuration: Configuration,
      val recipientLookup: RecipientLookup, val recipientRepository: RecipientRepository, val appConfig: ApplicationConfig)
      extends Controller with Secured with WithAnalytics with WithLogging {

   def findVerification(username: String) = Action.async{ _ =>
      println("IN TEST ONLY")
      recipientLookup.findRecipient(username.toLowerCase().trim).flatMap {
         case Some(recipient) =>
            recipientRepository.findVerificationHash(recipient).map {
               case Some(hash) =>
                  Ok.withHeaders(HeaderNames.LOCATION -> s"/recipient/${username.toLowerCase.trim}/verify/$hash/")
               case _ =>
                  println("IN TEST ONLY: No recipient found")
                  NotFound
            }
         case _ =>
            println("IN TEST ONLY: No recipient found.")
            println(s"IN TEST ONLY: Looking for [$username]")
            Future.successful(Conflict)
      }
   }

}
