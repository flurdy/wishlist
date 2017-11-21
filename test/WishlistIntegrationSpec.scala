package com.flurdy.wishlist

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.OptionValues._
import play.api.libs.ws.WSResponse
import scala.concurrent.{ExecutionContext, Future}
import com.flurdy.scalasoup.ScalaSoup


trait WishlistIntegrationHelper extends IntegrationHelper {

   def wishlistRootUrl(username: String)  = s"$baseUrl/recipient/${username.trim.toLowerCase}/wishlist"

   def createWishlist(username: String, session: Option[String]): Future[WSResponse] = {
      createWishlistWithTitle(username, "Some wishlist", session)
   }

   def createWishlistAndReturnId(username: String, session: Option[String])(implicit ec: ExecutionContext): Future[Option[Long]] =
      for {
         createResponse   <- createWishlist(username, session)
         wishlistLocation =  createResponse.header("Location").headOption
         wishlistId       =  wishlistLocation.map(_.split("/").last.toLong)
      } yield wishlistId

   def createWishlistWithTitle(username: String, title: String, session: Option[String]): Future[WSResponse] = {
      val createData = Map(
         "title" -> Seq(title),
         "description" -> Seq("Some description")
      )
      wsWithSession(wishlistRootUrl(username), session).withFollowRedirects(false).post(createData)
   }

   def removeWishlist(username: String, wishlistId: Long, session: Option[String]) = {
      val root = wishlistRootUrl(username)
      wsWithSession(s"$root/$wishlistId", session).withFollowRedirects(false).delete()
   }

   def showWishlist(username: String, wishlistId: Long, session: Option[String]) = {
      val root = wishlistRootUrl(username)
      wsWithSession(s"$root/$wishlistId/", session).withFollowRedirects(true).get()
   }

}

class WishlistIntegrationSpec extends AsyncFeatureSpec
      with GivenWhenThen with ScalaFutures with Matchers
      with IntegrationPatience with StartAndStopServer
      with RegistrationIntegrationHelper
      with LoginIntegrationHelper
      with CookieIntegrationHelper
      with WishlistIntegrationHelper
      with WishIntegrationHelper {

   info("As a wish recipient")
   info("I want a wishlist")
   info("so that I can list my wishes")

   feature("Wishlist flow") {

      scenario("Create wishlist") {

         val flow = for {
            _                <- register("Testerson")
            loginResponse    <- login("Testerson")
            session          =  findSessionCookie(loginResponse)
            createWishlistResponse <- createWishlist("Testerson", session)
            wishlistLocation =  createWishlistResponse.header("Location").headOption.value
            wishlistId       =  wishlistLocation.split("/").last.toLong
            showWishlistResponse   <- showWishlist("Testerson", wishlistId, session)
         } yield (session, createWishlistResponse, wishlistLocation, wishlistId, showWishlistResponse)

         flow map{ case(session, createWishlistResponse, wishlistLocation, wishlistId, showWishlistResponse) =>
            Given("a registered logged in recipient")
            session.value.length should be > 5

            When("creating a new wishlist")
            createWishlistResponse.status shouldBe 303
            findFlashCookie(createWishlistResponse, "messageSuccess").value shouldBe "Wishlist created"

            Then("should create wishlist")
            wishlistLocation shouldBe s"/testerson/wishlist/$wishlistId/"
            showWishlistResponse.status shouldBe 200
         }
      }

      scenario("List wishes in wishlist") {

         val flow = for {
            _             <- register("Testerson")
            loginResponse <- login("Testerson")
            session       =  findSessionCookie(loginResponse)
            createWishlistResponse <- createWishlist("Testerson", session)
            wishlistLocation = createWishlistResponse.header("Location").headOption.value
            wishlistId       = wishlistLocation.split("/").last.toLong
            showWishlistResponse1   <- showWishlist("Testerson", wishlistId, session)
            addWishResponse <- addWish("A handbag", wishlistId, "Testerson", session)
            showWishlistResponse2   <- showWishlist("Testerson", wishlistId, session)

         } yield (session, wishlistLocation, wishlistId, addWishResponse, showWishlistResponse2, showWishlistResponse1)


         flow map{ case (session, wishlistLocation, wishlistId, addWishResponse, showWishlistResponse2, showWishlistResponse1) =>

            Given("a registered logged in recipient")
            session.value.length should be > 5

            And("a wishlist")
            wishlistLocation shouldBe s"/testerson/wishlist/$wishlistId/"
            showWishlistResponse1.status shouldBe 200

            When("adding a wish to the wishlist")
            addWishResponse.status shouldBe 303

            Then("wish should be part of the wishlist")
            showWishlistResponse2.status shouldBe 200
            val s = ScalaSoup.parse(showWishlistResponse2.body).select(s"#wish-list .wish-row a").headOption
            s.value.text shouldBe "A handbag"
         }
      }

      scenario("Remove wishlist") {

         val flow = for {
            _             <- register("Testerson")
            loginResponse <- login("Testerson")
            session       =  findSessionCookie(loginResponse)
            createWishlistResponse <- createWishlist("Testerson", session)
            wishlistLocation = createWishlistResponse.header("Location").headOption.value
            wishlistId       = wishlistLocation.split("/").last.toLong
            showWishlistResponse1   <- showWishlist("Testerson", wishlistId, session)
            removeWishlistResponse  <- removeWishlist("Testerson", wishlistId, session)
            showWishlistResponse2   <- showWishlist("Testerson", wishlistId, session)
         } yield (session, wishlistLocation, wishlistId, removeWishlistResponse, showWishlistResponse1, showWishlistResponse2)

         flow map{ case(session, wishlistLocation, wishlistId, removeWishlistResponse, showWishlistResponse1, showWishlistResponse2) =>

            Given("a registered logged in recipient")
            session.value.length should be > 5

            And("a wishlist")
            wishlistLocation shouldBe s"/testerson/wishlist/$wishlistId/"
            showWishlistResponse1.status shouldBe 200

            When("deleting wishlist")
            removeWishlistResponse.status shouldBe 303
            findFlashCookie(removeWishlistResponse, "messageWarning").value shouldBe "Wishlist deleted"

            Then("wishlist should be removed")
            showWishlistResponse2.status shouldBe 404
         }
      }
   }
}
