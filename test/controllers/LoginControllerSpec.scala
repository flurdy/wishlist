package controllers

import akka.stream.Materializer
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any,anyString}
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Future
import com.flurdy.wishlist.ScalaSoup
import models._


class LoginControllerSpec extends BaseUnitSpec with Results with GuiceOneAppPerSuite {

   trait Setup {
      val configurationMock = mock[Configuration]
      val recipientLookupMock = mock[RecipientLookup]
      val controller = new LoginController(configurationMock, recipientLookupMock)
   }

   trait LoginSetup extends Setup {
      val recipientMock = mock[Recipient]
      val loginRequest = FakeRequest().withFormUrlEncodedBody(
         "username" -> "some-username",
         "password" -> "some-password")
   }

   "Login controller" when requesting {
      "[GET] /login.html" should {
         "show log in form" in new Setup {
            val result = controller.showLoginForm().apply(FakeRequest())

            status(result) mustBe 200
            val bodyDom = ScalaSoup.parse(contentAsString(result))
            bodyDom.select(s"#login-page").headOption mustBe defined
            bodyDom.select(s"form #inputUsername").headOption mustBe defined
         }
      }

      "[GET] /login/" should {
         "redirect to log in form" in new Setup {

            val result = controller.redirectToLoginForm().apply(FakeRequest())

            status(result) mustBe 303
            header("Location", result).value mustBe "/login.html"
         }
      }

      "[POST] /login/" should {
         "log in recipient given correct credentials" in new LoginSetup {

            when( recipientLookupMock.findRecipient("some-username") )
                  .thenReturn( Future.successful( Some(recipientMock) ) )
            when( recipientMock.authenticate("some-password") )
                  .thenReturn( Future.successful( true ) )
            when( recipientMock.isVerified )
                  .thenReturn( Future.successful( true ) )

            val result = controller.login().apply(loginRequest)

            status(result) mustBe 303
            header("Location", result).value mustBe "/"
            session(result).get("username").value mustBe "some-username"

            verify( recipientLookupMock ).findRecipient("some-username")
            verify( recipientMock ).authenticate("some-password")
            verify( recipientMock ).isVerified
         }

         "not log in recipient given incorrect credentials" in new LoginSetup {

            when( recipientLookupMock.findRecipient("some-username") )
                  .thenReturn( Future.successful( Some(recipientMock) ) )
            when( recipientMock.authenticate("some-password") )
                  .thenReturn( Future.successful( false ) )

            val result = controller.login().apply(loginRequest)

            status(result) mustBe 400
            session(result).get("username") mustBe None

            verify( recipientMock, never ).isVerified
         }

         "not log in recipient given unknown username" in new LoginSetup {

            when( recipientLookupMock.findRecipient("some-username") )
                  .thenReturn( Future.successful( None ) )

            val result = controller.login().apply(loginRequest)

            status(result) mustBe 400
            session(result).get("username") mustBe None

            verifyZeroInteractions( recipientMock )
         }

         "not log in recipient given not verified email address" in new LoginSetup {

            when( recipientLookupMock.findRecipient("some-username") )
                  .thenReturn( Future.successful( Some(recipientMock) ) )
            when( recipientMock.authenticate("some-password") )
                  .thenReturn( Future.successful( true ) )
            when( recipientMock.isVerified )
                  .thenReturn( Future.successful( false ) )

            val result = controller.login().apply(loginRequest)

            status(result) mustBe 400
            session(result).get("username") mustBe None

            verify( recipientMock ).isVerified
         }

         "log in in a new session if already logged in" in new LoginSetup { pending }
      }

   }
}
