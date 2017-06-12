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
      val controller = new Application(configurationMock, mock[RecipientLookup])
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
   }
}
