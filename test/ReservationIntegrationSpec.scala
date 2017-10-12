package com.flurdy.wishlist

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.OptionValues._
import play.api.libs.ws.WSResponse
import scala.concurrent.ExecutionContext


trait ReservationIntegrationHelper extends RegistrationIntegrationHelper
      with LoginIntegrationHelper
      with CookieIntegrationHelper
      with WishlistIntegrationHelper
      with WishIntegrationHelper {

   def createRecipientWishlist(username: String)(implicit ec: ExecutionContext) =
      for {
         session          <- registerAndLogin("Testerson")
         createWishlistResponse <- createWishlist("Testerson", session)
         wishlistLocation =  createWishlistResponse.header("Location").headOption.value
         wishlistId       =  wishlistLocation.split("/").last.toLong
      } yield (session, wishlistId)

   def createWishlistWithWish(username: String, wishTitle: String)(implicit ec: ExecutionContext) =
      for {
         (session, wishlistId) <- createRecipientWishlist(username)
         addWishResponse  <- addWish("A handbag", wishlistId, "Testerson", session)
         wishLocation     =  addWishResponse.header("Location").headOption.value
         wishId           =  wishLocation.split("""\?wish=""").last.toLong
      } yield (session, wishlistId, addWishResponse, wishId)

   def reserve(username: String, wishlistId: Long, wishId: Long, session: Option[String]) = {
      val root = wishlistRootUrl(username)
      wsWithSession(s"$root/$wishlistId/wish/$wishId/reserve", session)
            .withFollowRedirects(false).post("")
   }

   def unreserve(username: String, wishlistId: Long, wishId: Long, session: Option[String]) = {
      val root = wishlistRootUrl(username)
      wsWithSession(s"$root/$wishlistId/wish/$wishId/unreserve", session)
            .withFollowRedirects(false).post("")
   }
}


class ReservationIntegrationSpec extends AsyncFeatureSpec
      with GivenWhenThen with ScalaFutures with Matchers
      with IntegrationPatience with StartAndStopServer
      with ReservationIntegrationHelper {

   info("As a wish giver")
   info("I want to reserve wishes")
   info("so that I can give them to the recipient")

   feature("Reserve flow") {

      scenario("find a wish without logging in") {

         val flow = for {
            (_, wishlistId, addWishResponse, _)  <- createWishlistWithWish("Testerson", "A handbag")
            showWishlistResponse <- showWishlist("Testerson", wishlistId, session = None)
         } yield (addWishResponse, showWishlistResponse)

         flow map { case (addWishResponse, showWishlistResponse) =>
            Given("a recipient with a wishlist with wishes")
            addWishResponse.status shouldBe 303

            When("looking up a wishlist")
            showWishlistResponse.status shouldBe 200

            Then("an non logged in recipient should see it")
            val s = ScalaSoup.parse(showWishlistResponse.body).select(s"#wish-list .wish-row a").headOption
            s.value.text shouldBe "A handbag"
         }

      }

      scenario("find a wish when logged in")  {

         val flow = for {
            (_, wishlistId, addWishResponse, _)  <- createWishlistWithWish("Testerson", "A handbag")
            sessionGrandma   <- registerAndLogin("Grandma")
            showWishlistResponse <- showWishlist("Testerson", wishlistId, sessionGrandma)
         } yield (addWishResponse, sessionGrandma, showWishlistResponse)

         flow map { case (addWishResponse, sessionGrandma, showWishlistResponse) =>
            Given("a recipient with a wishlist with wishes")
            addWishResponse.status shouldBe 303

            And("a another logged in user")
            sessionGrandma shouldBe defined

            When("looking up a wishlist")
            showWishlistResponse.status shouldBe 200

            Then("an non logged in recipient should see it")
            val s = ScalaSoup.parse(showWishlistResponse.body).select(s"#wish-list .wish-row a").headOption
            s.value.text shouldBe "A handbag"
         }

      }

      scenario("reserve a wish") {
         val flow = for {
            (_, wishlistId, addWishResponse, wishId)  <- createWishlistWithWish("Testerson", "A handbag")
            sessionGrandma   <- registerAndLogin("Grandma")
            showWishlistResponse1 <- showWishlist("Testerson", wishlistId, sessionGrandma)
            reserveResponse       <- reserve("Testerson", wishlistId, wishId, sessionGrandma)
            showWishlistResponse2 <- showWishlist("Testerson", wishlistId, sessionGrandma)
         } yield (addWishResponse, sessionGrandma, showWishlistResponse1, reserveResponse, showWishlistResponse2)

         flow map { case (addWishResponse, sessionGrandma, showWishlistResponse1, reserveResponse, showWishlistResponse2) =>
            Given("a recipient with a wishlist with wishes")
            addWishResponse.status shouldBe 303

            And("a another logged in user")
            sessionGrandma shouldBe defined
            addWishResponse.status shouldBe 303

            And("a wish is not reserved")
            val reservedBefore = ScalaSoup.parse(showWishlistResponse1.body)
                  .select(s"#wish-list .wish-row .reserved").headOption
            reservedBefore shouldBe None

            When("someone reserves a wish")
            reserveResponse.status shouldBe 303

            Then("a wish should be marked reserved")
            val reservedAfter = ScalaSoup.parse(showWishlistResponse2.body)
                  .select(s"#wish-list .wish-row .reserved").headOption
            reservedAfter.value.text shouldBe "reserved"
         }
      }

      scenario("unreserve a wish") {
         val flow = for {
            (_, wishlistId, addWishResponse, wishId)  <- createWishlistWithWish("Testerson", "A handbag")
            sessionGrandma        <- registerAndLogin("Grandma")
            _                     <- reserve("Testerson", wishlistId, wishId, sessionGrandma)
            showWishlistResponse1 <- showWishlist("Testerson", wishlistId, sessionGrandma)
            unreserveResponse     <- unreserve("Testerson", wishlistId, wishId, sessionGrandma)
            showWishlistResponse2 <- showWishlist("Testerson", wishlistId, sessionGrandma)
         } yield (addWishResponse, sessionGrandma, showWishlistResponse1, unreserveResponse, showWishlistResponse2)

         flow map { case (addWishResponse, sessionGrandma, showWishlistResponse1, unreserveResponse, showWishlistResponse2) =>
            Given("a recipient with a wishlist with wishes")
            addWishResponse.status shouldBe 303

            And("a another logged in user")
            sessionGrandma shouldBe defined
            addWishResponse.status shouldBe 303

            And("a wish which is reserved")
            val reservedBefore = ScalaSoup.parse(showWishlistResponse1.body)
                  .select(s"#wish-list .wish-row .reserved").headOption
            reservedBefore.value.text shouldBe "reserved"

            When("someone unreserves a wish")
            unreserveResponse.status shouldBe 303

            Then("a wish should be marked unreserved")
            val reservedAfter = ScalaSoup.parse(showWishlistResponse2.body)
                  .select(s"#wish-list .wish-row .reserved").headOption
            reservedAfter shouldBe None
         }
      }

   }
}
