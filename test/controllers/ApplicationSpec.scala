package controllers

import akka.stream.Materializer
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite 
import play.api.{Application => PlayApplication, Configuration}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.{ExecutionContext, Future}
import com.flurdy.scalasoup.ScalaSoup
import models._
import repositories._


trait BaseUnitSpec extends PlaySpec with MockitoSugar with ScalaFutures {
     def beAbleTo    = afterWord("be able to")
     def calling     = afterWord("calling")
     def _contain     = afterWord("contain")
     def does        = afterWord("does")
     def is          = afterWord("is")
     def fail        = afterWord("fail")
     def has         = afterWord("has")
     def given       = afterWord("given")
     def notBeAbleTo = afterWord("not be able to")
     def pass        = afterWord("pass")
     def posting     = afterWord("posting")
     def redirect    = afterWord("redirect")
     def requesting  = afterWord("requesting")
     def respondWith = afterWord("respond with")
     def show        = afterWord("show")
}

trait BaseControllerSpec extends BaseUnitSpec with Results with GuiceOneAppPerSuite {

   trait WithApp {   
      val controllerComponents = app.injector.instanceOf[ControllerComponents]
      val usernameAction = app.injector.instanceOf[UsernameAction]
      val maybeCurrentRecipientAction = app.injector.instanceOf[MaybeCurrentRecipientAction]
      implicit val executionContext = controllerComponents.executionContext
   }

   trait WithMock {
      def application: PlayApplication
      lazy val controllerComponents = application.injector.instanceOf[ControllerComponents]
      lazy val usernameAction = application.injector.instanceOf[UsernameAction]
      lazy val maybeCurrentRecipientAction = application.injector.instanceOf[MaybeCurrentRecipientAction]
      implicit lazy val executionContext = controllerComponents.executionContext

   }

}


class ApplicationSpec extends BaseControllerSpec {

   trait Setup extends WithMock {
      val appConfigMock = mock[ApplicationConfig]
      implicit val wishlistRepositoryMock = mock[WishlistRepository]
      implicit val recipientRepositoryMock = mock[RecipientRepository]
      val recipientLookupMock = mock[RecipientLookup]

      val application = new GuiceApplicationBuilder()
            .overrides(bind[RecipientLookup].toInstance(recipientLookupMock))
            .build()

      val controller = new Application(
            controllerComponents,
            recipientLookupMock,
            appConfigMock,
            usernameAction,
            maybeCurrentRecipientAction)
            (executionContext,
            wishlistRepositoryMock,
            recipientRepositoryMock)

      when(appConfigMock.findString(anyString)).thenReturn(None)
   }

   "Application controller" when requesting {
      "[GET] /" should {
         "show index page" which is {

            "anonymous given not logged in" in new Setup {

               val result = controller.index().apply(FakeRequest())

               status(result) mustBe 200
               val bodyDom = ScalaSoup.parse(contentAsString(result))
               bodyDom.select("#index-page").headOption mustBe defined
               bodyDom.select("#wishlist-list").headOption mustBe None
               bodyDom.select("#login-box").headOption mustBe defined
            }

            "logged in given recipient is logged in" in new Setup {

               val recipientMock = mock[Recipient]
               val wishlist = new Wishlist("my list" , recipientMock)
               val wishlists = List(wishlist)
               when(recipientLookupMock.findRecipient("some-username"))
                     .thenReturn(Future.successful(Some(recipientMock)))
               when(recipientRepositoryMock.findRecipient("some-username"))
                     .thenReturn(Future.successful(Some(recipientMock)))
               when(recipientMock.username).thenReturn("some-username")
               when(recipientMock.findAndInflateWishlists(eqTo(wishlistRepositoryMock), eqTo(recipientRepositoryMock), any[ExecutionContext]))
                     .thenReturn(Future.successful(wishlists))
               when(recipientMock.inflate(eqTo(recipientRepositoryMock), any[ExecutionContext]))
                     .thenReturn(Future.successful(recipientMock))

               val result = controller.index().apply(FakeRequest().withSession("username"  -> "some-username"))

               status(result) mustBe 200
               val bodyDom = ScalaSoup.parse(contentAsString(result))
               bodyDom.select("#index-page").headOption mustBe defined
               bodyDom.select("#wishlist-list").headOption mustBe defined
               bodyDom.select("#login-box").headOption mustBe None
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
            bodyDom.select("#about-page").headOption mustBe defined
         }
      }
   }
}
