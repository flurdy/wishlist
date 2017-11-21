package com.flurdy.wishlist

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.OptionValues._
import play.api.libs.ws.WSResponse
import scala.concurrent.{ExecutionContext, Future}
import com.flurdy.scalasoup.ScalaSoup


trait WishIntegrationHelper extends IntegrationHelper {

   def wishlistRootUrl(username: String): String

   def registerAndLogin(username: String)(implicit ec: ExecutionContext): Future[Option[String]]

   def createWishlistAndReturnId(username: String, session: Option[String])(implicit ec: ExecutionContext): Future[Option[Long]]

   def addWish(wishTitle: String, wishlistId: Long, username: String, session: Option[String]): Future[WSResponse] = {
      val root = wishlistRootUrl(username)
      val wishData = Map("title" -> Seq(wishTitle))
      wsWithSession(s"$root/$wishlistId/wish", session).withFollowRedirects(false).post(wishData)
   }

   def updateWish(newWishTitle: String, wishId: Long, wishlistId: Long, username: String, session: Option[String]): Future[WSResponse] = {
      val root = wishlistRootUrl(username)
      val wishData = Map("title" -> Seq(newWishTitle))
      wsWithSession(s"$root/$wishlistId/wish/$wishId", session).withFollowRedirects(false).put(wishData)
   }

   def moveWish(targetWishlistId: Long, wishId: Long, sourceWishlistId: Long, username: String, session: Option[String]): Future[WSResponse] = {
      val root = wishlistRootUrl(username)
      val wishData = Map("targetwishlistid" -> Seq(s"$targetWishlistId"))
      wsWithSession(s"$root/$sourceWishlistId/wish/$wishId/move", session).withFollowRedirects(false).post(wishData)
   }

   def deleteWish(wishId: Long, wishlistId: Long, username: String, session: Option[String]): Future[WSResponse] = {
      val root = wishlistRootUrl(username)
      wsWithSession(s"$root/$wishlistId/wish/$wishId", session).withFollowRedirects(false).delete
   }

   def registerCreateWishlistAndAddWish(wishTitle: String, username: String)(implicit ec: ExecutionContext): Future[(Option[String], Option[Long], Option[Long])] =
      for {
         session    <- registerAndLogin("Testerson")
         wishlistId <- createWishlistAndReturnId("Testerson", session)
         response   <- addWish(wishTitle, wishlistId.value, username, session)
         wishLocation = response.header("Location").headOption
         wishId       = wishLocation.map(_.split("=").last.toLong)
      } yield (session, wishlistId, wishId)
}

class WishIntegrationSpec extends AsyncFeatureSpec
      with GivenWhenThen with ScalaFutures with Matchers
      with IntegrationPatience with StartAndStopServer
      with RegistrationIntegrationHelper
      with LoginIntegrationHelper
      with CookieIntegrationHelper
      with WishlistIntegrationHelper
      with WishIntegrationHelper {

   info("As a wish recipient")
   info("I want manage a wish")
   info("so that I can ensure my wishes are informative")

   feature("Wish flow") {

      scenario("Add a wish"){

         val flow = for {
            session                 <- registerAndLogin("Testerson")
            wishlistId              <- createWishlistAndReturnId("Testerson", session)
            addWishResponse         <- addWish("A handbag", wishlistId.value, "Testerson", session)
            showWishlistResponse    <- showWishlist("Testerson", wishlistId.value, session)
         } yield (session, wishlistId, addWishResponse, showWishlistResponse)

         flow map{ case(session, wishlistId, addWishResponse, showWishlistResponse) =>

            Given("a registered logged in recipient")
            session.value.length should be > 5

            And("a wishlist")
            showWishlistResponse.status shouldBe 200

            When("adding a wish to the wishlist")
            addWishResponse.status shouldBe 303

            Then("wish should be part of the wishlist")
            val s = ScalaSoup.parse(showWishlistResponse.body)
                        .select(s"#wish-list .wish-row a").headOption
            s.value.text shouldBe "A handbag"
         }
      }


      scenario("Update wish"){

         val flow = for {
            (session, wishlistId, wishId) <- registerCreateWishlistAndAddWish("A handbag", "Testerson")
            updateWishResponse   <- updateWish("A monkey foot", wishId.value, wishlistId.value, "Testerson", session)
            showWishlistResponse <- showWishlist("Testerson", wishlistId.value, session)
         } yield (session, wishlistId, wishId, updateWishResponse, showWishlistResponse)

         flow map{ case(session, wishlistId, wishId, updateWishResponse, showWishlistResponse) =>

            Given("a registered logged in recipient")
            session.value.length should be > 5

            And("a wishlist")
            wishlistId.value should be > 0l

            And("a wish")
            wishId.value should be > 0l

            When("updating wish")
            updateWishResponse.status shouldBe 303

            Then("wish should be updated")
            val s = ScalaSoup.parse(showWishlistResponse.body)
                        .select(s"#wish-list .wish-row a").headOption
            s.value.text shouldBe "A monkey foot"
         }
      }

      scenario("Move a wish"){

         val flow = for {
            (session, sourceWishlistId, wishId) <- registerCreateWishlistAndAddWish("A handbag", "Testerson")
            targetWishlistId <- createWishlistAndReturnId("Testerson", session)
            targetWishlistResponse1 <- showWishlist("Testerson", targetWishlistId.value, session)
            moveWishResponse   <- moveWish(targetWishlistId.value, wishId.value, sourceWishlistId.value, "Testerson", session)
            sourceWishlistResponse  <- showWishlist("Testerson", sourceWishlistId.value, session)
            targetWishlistResponse2 <- showWishlist("Testerson", targetWishlistId.value, session)
         } yield (session, sourceWishlistId, wishId, targetWishlistId, targetWishlistResponse1, moveWishResponse, sourceWishlistResponse, targetWishlistResponse2)

         flow map{ case(session, sourceWishlistId, wishId, targetWishlistId, targetWishlistResponse1, moveWishResponse, sourceWishlistResponse, targetWishlistResponse2) =>

            Given("a registered logged in recipient")
            session.value.length should be > 5

            And("a wishlist")
            sourceWishlistId.value should be > 0l

            And("a wish")
            wishId.value should be > 0l

            And("another wishlist")
            targetWishlistId.value should be > 0l
            // targetWishlistId.value should not be sourceWishlistId.value
            val targetOriginalBody = ScalaSoup.parse(targetWishlistResponse1.body)
                        .select(s"#wish-list .wish-row a").headOption
            targetOriginalBody shouldBe None

            When("moving wish to the other wishlist")
            moveWishResponse.status shouldBe 303

            Then("wish should not be on source wishlist")
            val sourceBody = ScalaSoup.parse(sourceWishlistResponse.body)
                        .select(s"#wish-list .wish-row a").headOption
            sourceBody shouldBe None

            Then("wish should be on target wishlist")
            val targetFinalBody = ScalaSoup.parse(targetWishlistResponse2.body)
                        .select(s"#wish-list .wish-row a").headOption
            targetFinalBody.value.text shouldBe "A handbag"
         }
      }

      scenario("Delete a wish"){

         val flow = for {
            (session, wishlistId, wishId) <- registerCreateWishlistAndAddWish("A handbag", "Testerson")
            deleteWishResponse   <- deleteWish( wishId.value, wishlistId.value, "Testerson", session)
            showWishlistResponse <- showWishlist("Testerson", wishlistId.value, session)
         } yield (session, wishlistId, wishId, deleteWishResponse, showWishlistResponse)

         flow map{ case(session, wishlistId, wishId, deleteWishResponse, showWishlistResponse) =>

            Given("a registered logged in recipient")
            session.value.length should be > 5

            And("a wishlist")
            wishlistId.value should be > 0l

            And("a wish")
            wishId.value should be > 0l

            When("deleting a wish")
            deleteWishResponse.status shouldBe 303

            Then("wish should be removed")
            val s = ScalaSoup.parse(showWishlistResponse.body)
                        .select(s"#wish-list .wish-row a").headOption
            s shouldBe None
         }
      }
   }

   feature("Wish links") {
      scenario("Add link to wish")(pending)
      scenario("Remove link from wish")(pending)

   }
}
