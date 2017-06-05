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

trait BaseUnitSpec extends PlaySpec with MockitoSugar with ScalaFutures {
     def is = afterWord("is")
     def requesting = afterWord("requesting")
     def posting    = afterWord("posting")
     def show       = afterWord("show")
     def redirect   = afterWord("redirect")
}


class ApplicationSpec extends BaseUnitSpec with Results with GuiceOneAppPerSuite {

   trait Setup {
      val configurationMock = mock[Configuration]
      val recipientFactoryMock = mock[RecipientFactory]
      val recipientLookupMock = mock[RecipientLookup]
      val controller = new Application(configurationMock, recipientFactoryMock, recipientLookupMock)
   }

   "Application controller" when requesting {
      "[GET] /" should {
         "show index page" which is {
            "anonymous given not logged in" in new Setup {

               val result = controller.index().apply(FakeRequest())

               status(result) mustBe 200
               val bodyDom = ScalaSoup.parse(contentAsString(result))
               bodyDom.select(s"#index-page").headOption mustBe defined
            }
            "logged in given recipient is logged in" in new Setup {

               val result = controller.index().apply(FakeRequest())

               pending
            }
         }
      }

      "[GET] /index.html" should {
         "redirect to index page" in new Setup {

            val result = controller.redirectToIndex().apply(FakeRequest())

            status(result) mustBe 303
            header("Location", result).value mustBe "/"
         }
      }

      "[GET] /about.html" should {
         "show about page" in new Setup {

            val result = controller.about().apply(FakeRequest())

            status(result) mustBe 200
            val bodyDom = ScalaSoup.parse(contentAsString(result))
            bodyDom.select(s"#about-page").headOption mustBe defined
         }
      }

      "[GET] /contact.html" should {
         "show contact page" in new Setup {

            val result = controller.contact().apply(FakeRequest())

            status(result) mustBe 200
            val bodyDom = ScalaSoup.parse(contentAsString(result))
            bodyDom.select(s"#contact-page").headOption mustBe defined
         }
      }

      "[GET] /contact/" should {
         "redirect to contact page" in new Setup {

            val result = controller.redirectToContact().apply(FakeRequest())

            status(result) mustBe 303
            header("Location", result).value mustBe "/contact.html"
         }
      }

      "[POST] /contact" should {
         "send message" ignore { // in new Setup {
            pending
         }
      }
   }
}

class RegisterControllerSpec extends BaseUnitSpec with Results with GuiceOneAppPerSuite {

   trait Setup {
      val configurationMock = mock[Configuration]
      val recipientFactoryMock = mock[RecipientFactory]
      val recipientLookupMock = mock[RecipientLookup]
      val controller = new RegisterController(configurationMock, recipientFactoryMock, recipientLookupMock)
   }

   "Register controller" when requesting {

      "[GET] /register.html" should {
         "prefill the register form" in new Setup {
            val request = FakeRequest().withFormUrlEncodedBody("email" -> "some-username")

            val result = controller.redirectToRegisterForm().apply(request)

            status(result) mustBe 200

            val bodyDom = ScalaSoup.parse(contentAsString(result))
            bodyDom.select(s"#register-page").headOption mustBe defined
            bodyDom.select(s"form #inputUsername").headOption.value.attr("value") mustBe "some-username"
            bodyDom.select(s"form #inputEmail").headOption.value.attr("value") mustBe "some-username"
         }
      }

      "[GET] /register/" should {
         "show register form" in new Setup {

            val result = controller.redirectToRegisterForm().apply(FakeRequest())

            status(result) mustBe 200

            val bodyDom = ScalaSoup.parse(contentAsString(result))
            bodyDom.select(s"#register-page").headOption mustBe defined
            bodyDom.select(s"form #inputUsername").headOption.value.attr("value") mustBe ""
         }

         "not show register form given a logged in session" in new Setup {
            pending
         }
      }

      "[POST] /register/" must {
         "register" which {
            "creates a recipient" in new Setup {

               val recipientMock = mock[Recipient]
               val registerForm = ("some-username", Some("some name"), "some-email@example.com", "some-password", "some-password")
               when ( recipientFactoryMock.newRecipient( registerForm ) ).thenReturn( recipientMock )
               when ( recipientMock.save() ).thenReturn( Future.successful( Right(recipientMock) ) )

               val request = FakeRequest().withFormUrlEncodedBody(
                  "fullname" -> "some name",
                  "username" -> "some-username",
                  "email"    -> "some-email@example.com",
                  "password" -> "some-password",
                  "confirm"  -> "some-password")

               val result = controller.register().apply(request)

               status(result) mustBe 303
               header("Location", result).value mustBe "/"

               verify ( recipientMock ).save()

            }

            "send verification email" in new Setup {
               // verify(email notifier mock, times(1)).send
               pending
            }
         }
      }
   }

}

class LoginControllerSpec extends BaseUnitSpec with Results with GuiceOneAppPerSuite {

   trait Setup {
      val configurationMock = mock[Configuration]
      val recipientFactoryMock = mock[RecipientFactory]
      val recipientLookupMock = mock[RecipientLookup]
      val controller = new LoginController(configurationMock, recipientFactoryMock, recipientLookupMock)
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
      }

   }
}
