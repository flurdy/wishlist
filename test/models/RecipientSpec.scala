package models

import com.github.t3hnar.bcrypt._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any,anyString}
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models._
import repositories.RecipientRepository
import controllers.BaseUnitSpec


class RecipientSpec extends BaseUnitSpec with IntegrationPatience with ScalaFutures {

   trait Setup {
      val recipient = Recipient(
         recipientId = Some(1001),
         username = "someuser",
         fullname = Some("Some Name"),
         email = "some@example.com",
         password = None,
         isAdmin = false)
      val correctPassword = "some-password"
      val wrongPassword = "wrong-password"
      val encryptedCorrectPassword = correctPassword.bcrypt
      implicit val recipientRepositoryMock = mock[RecipientRepository]

      when( recipientRepositoryMock.findCredentials(recipient) )
         .thenReturn(
            Future.successful(
               Some(encryptedCorrectPassword)))
   }

   "authenticate" should {
      "authenticate" when given {
         "valid credentials" in new Setup {
            whenReady( recipient.authenticate(correctPassword)){ response =>
               response mustBe true
            }
         }
      }
      "not authenticate" when given {
         "invalid credentials" in new Setup {
            whenReady( recipient.authenticate(wrongPassword)){ response =>
               response mustBe false
            }
         }
      }
   }

}
