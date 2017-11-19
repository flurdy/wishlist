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
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Future
import com.flurdy.wishlist.ScalaSoup
import models._
import notifiers._
import repositories._


class ContactControllerSpec extends BaseControllerSpec {

   trait Setup extends WithMock {
      val appConfigMock       = mock[ApplicationConfig]
      val notifierMock        = mock[EmailNotifier]
      val recipientMock       = mock[Recipient]
      val recipientLookupMock = mock[RecipientLookup]

      val application = new GuiceApplicationBuilder()
            .overrides(bind[RecipientLookup].toInstance(recipientLookupMock))
            .build()

      val controller = new ContactController(controllerComponents, 
            recipientLookupMock, notifierMock, appConfigMock,
            usernameAction, maybeCurrentRecipientAction)(executionContext)

      when(appConfigMock.findString(anyString)).thenReturn(None)

      when(recipientLookupMock.findRecipient("some-username")(executionContext)).thenReturn(Future.successful(Some(recipientMock)))
   }

   "Contact controller" when requesting {

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
         "send message" when {
            "not logged in" in new Setup {

               when( notifierMock.sendContactEmail(
                     "some name", "some@example.com", Some("some-username"), Some("some subject"), "some message", None) )
                  .thenReturn( Future.successful(()) )


               val contactRequest = FakeRequest().withFormUrlEncodedBody(
                  "name"     -> "some name",
                  "email"    -> "some@example.com",
                  "username" -> "some-username",
                  "subject"  -> "some subject",
                  "message"  -> "some message" )

               val result = controller.sendContact().apply(contactRequest)

               status(result) mustBe 303
               header("Location", result).value mustBe "/"

               verify( notifierMock ).sendContactEmail(
                     "some name", "some@example.com", Some("some-username"), Some("some subject"), "some message", None)
            }

            "logged in" in new Setup {

               when( notifierMock.sendContactEmail(
                     "some name", "some@example.com", Some("some-username"), Some("some subject"), "some message", Some(recipientMock)))
                  .thenReturn( Future.successful(()) )


               val contactRequest = FakeRequest()
                  .withFormUrlEncodedBody(
                     "name"     -> "some name",
                     "email"    -> "some@example.com",
                     "username" -> "some-username",
                     "subject"  -> "some subject",
                     "message"  -> "some message" )
                  .withSession("username"  -> "some-username")

               val result = controller.sendContact().apply(contactRequest)

               status(result) mustBe 303
               header("Location", result).value mustBe "/"

               verify( notifierMock ).sendContactEmail(
                     "some name", "some@example.com", Some("some-username"), Some("some subject"), "some message", Some(recipientMock))
            }

            "no username filled" in new Setup {

               when( notifierMock.sendContactEmail(
                     "some name", "some@example.com", Some("some-username"), Some("some subject"), "some message", None) )
                  .thenReturn( Future.successful(()) )


               val contactRequest = FakeRequest().withFormUrlEncodedBody(
                  "name"     -> "some name",
                  "email"    -> "some@example.com",
                  "username" -> "some-username",
                  "subject"  -> "some subject",
                  "message"  -> "some message" )

               val result = controller.sendContact().apply(contactRequest)

               status(result) mustBe 303
               header("Location", result).value mustBe "/"

               verify( notifierMock ).sendContactEmail(
                     "some name", "some@example.com", Some("some-username"), Some("some subject"), "some message", None)
            }

            "no subject filled" in new Setup {

               when( notifierMock.sendContactEmail(
                     "some name", "some@example.com", Some("some-username"), Some("some subject"), "some message", None) )
                  .thenReturn( Future.successful(()) )


               val contactRequest = FakeRequest().withFormUrlEncodedBody(
                  "name"     -> "some name",
                  "email"    -> "some@example.com",
                  "username" -> "some-username",
                  "subject"  -> "some subject",
                  "message"  -> "some message" )

               val result = controller.sendContact().apply(contactRequest)

               status(result) mustBe 303
               header("Location", result).value mustBe "/"

               verify( notifierMock ).sendContactEmail(
                     "some name", "some@example.com", Some("some-username"), Some("some subject"), "some message", None)
            }
         }

         "not send mesage" when {
            "name is missing" in new Setup {

               val contactRequest = FakeRequest().withFormUrlEncodedBody(
                  "name"     -> "",
                  "email"    -> "some@example.com",
                  "username" -> "some-username",
                  "subject"  -> "some subject",
                  "message"  -> "some message" )

               val result = controller.sendContact().apply(contactRequest)

               status(result) mustBe 400
               verifyZeroInteractions( notifierMock )
            }
            "email is missing" in new Setup {

               val contactRequest = FakeRequest().withFormUrlEncodedBody(
                  "name"     -> "some name",
                  "email"    -> "",
                  "username" -> "some-username",
                  "subject"  -> "some subject",
                  "message"  -> "some message" )

               val result = controller.sendContact().apply(contactRequest)

               status(result) mustBe 400
               verifyZeroInteractions( notifierMock )
            }

            "message is missing" in new Setup {

               val contactRequest = FakeRequest().withFormUrlEncodedBody(
                  "name"     -> "some name",
                  "email"    -> "some@example.com",
                  "username" -> "some-username",
                  "subject"  -> "some subject" )

               val result = controller.sendContact().apply(contactRequest)

               status(result) mustBe 400
               verifyZeroInteractions( notifierMock )
          }
         }
      }
   }
}
