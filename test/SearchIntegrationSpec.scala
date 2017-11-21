package com.flurdy.wishlist

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.OptionValues._
import play.api.libs.ws.WSResponse
import com.flurdy.scalasoup.ScalaSoup


trait SearchIntegrationHelper extends IntegrationHelper {

   val searchUrl = s"$baseUrl/search"

   def search(searchTerm: String) =
      getWsClient().url(s"$searchUrl?term=$searchTerm").withFollowRedirects(false).get()

   def searchWithSession(searchTerm: String, session: Option[String]) =
      wsWithSession(s"$searchUrl?term=$searchTerm", session).withFollowRedirects(false).get()

}

class SearchIntegrationSpec extends AsyncFeatureSpec
      with GivenWhenThen with ScalaFutures with Matchers
      with IntegrationPatience with StartAndStopServer
      with RegistrationIntegrationHelper
      with LoginIntegrationHelper
      with FrontPageIntegrationHelper
      with CookieIntegrationHelper
      with WishlistIntegrationHelper
      with SearchIntegrationHelper {

   info("As a wish reserver")
   info("I want to search for wishlists")
   info("so that I can find my friends wishes")

   feature("search flow") {

      scenario("find wishlists when not logged in"){

         val flow = for {
            _              <- register("Testerson")
            loginResponse  <- login("Testerson")
            session        =  findSessionCookie(loginResponse)
            createResponse <- createWishlistWithTitle("Testerson", "Some wishlist", session)
            indexResponse  <- frontpage()
            searchResponse <- search("Some wishlist")
         } yield (createResponse, indexResponse, searchResponse)

         flow map { case (createResponse, indexResponse, searchResponse) =>
            Given("A non logged in user")
            val indexBox = ScalaSoup.parse(indexResponse.body).select("#login-box input").headOption
            indexBox shouldBe defined

            And("a wishlist from another user")
            createResponse.status shouldBe 303
            findFlashCookie(createResponse, "messageSuccess").value shouldBe "Wishlist created"

            When("searching for that wishlist")
            searchResponse.status shouldBe 200
            val searchBody = ScalaSoup.parse(searchResponse.body)
            val wishlistPage = searchBody.select("#list-wishlists-page").headOption
            wishlistPage shouldBe defined

            Then("should find it")
            val wishlistBox = searchBody.select("#wishlist-list").headOption
            wishlistBox shouldBe defined

            val wishlistRow = searchBody.select("#wishlist-list td:eq(0) a").headOption
            wishlistRow shouldBe defined
            wishlistRow.value.text shouldBe "Some wishlist"

            val wishlistRow2 = searchBody.select("#wishlist-list td:eq(1) a").headOption
            wishlistRow2 shouldBe defined
            wishlistRow2.value.text shouldBe "testerson"
         }
      }

      scenario("find your wishlists when logged in") {
         val flow = for {
            _ <- register("Testerson2")
            loginResponse <- login("Testerson2")
            session = findSessionCookie(loginResponse)
            createResponse <- createWishlistWithTitle("Testerson2", "Some other wishlist", session)
            indexResponse <- frontpageWithSession(session)
            searchResponse <- searchWithSession("Some oTHer wishlist", session)
         } yield (createResponse, indexResponse, searchResponse)

         flow map { case (createResponse, indexResponse, searchResponse) =>
            Given("A logged in user")
            val logoutLink = ScalaSoup.parse(indexResponse.body).select("#logout-box li a").headOption
            logoutLink.value.text shouldBe "testerson2"

            And("a wishlist from another user")
            createResponse.status shouldBe 303
            findFlashCookie(createResponse, "messageSuccess").value shouldBe "Wishlist created"

            When("searching for that wishlist")
            searchResponse.status shouldBe 200
            val searchBody = ScalaSoup.parse(searchResponse.body)
            val wishlistPage = searchBody.select(s"#list-wishlists-page").headOption
            wishlistPage shouldBe defined

            Then("should find it")
            val wishlistRow = searchBody.select(s"#wishlist-list td a").headOption
            wishlistRow.value.text shouldBe "Some other wishlist"

            val wishlistRow2 = searchBody.select(s"#wishlist-list td:eq(1) a").headOption
            wishlistRow2 shouldBe defined
            wishlistRow2.value.text shouldBe "testerson2"
         }
      }

      scenario("find other's wishlists when logged in") {
         val flow = for {
            _ <- register("Testerson3")
            loginResponse1 <- login("Testerson3")
            session1 = findSessionCookie(loginResponse1)
            createResponse <- createWishlistWithTitle("Testerson3", "Some third wishlist", session1)
            _ <- register("Testerson4")
            loginResponse2 <- login("Testerson4")
            session2 = findSessionCookie(loginResponse2)
            indexResponse  <- frontpageWithSession(session2)
            searchResponse <- searchWithSession("Some third wishlist",session2)
         } yield (createResponse, indexResponse, searchResponse)

         flow map { case (createResponse, indexResponse, searchResponse) =>
            Given("A logged in user")
            val logoutLink = ScalaSoup.parse(indexResponse.body).select("#logout-box li a").headOption
            logoutLink.value.text shouldBe "testerson4"

            And("a wishlist from another user")
            createResponse.status shouldBe 303
            findFlashCookie(createResponse, "messageSuccess").value shouldBe "Wishlist created"

            When("searching for that wishlist")
            searchResponse.status shouldBe 200
            val searchBody = ScalaSoup.parse(searchResponse.body)
            val wishlistPage = searchBody.select(s"#list-wishlists-page").headOption
            wishlistPage shouldBe defined

            Then("should find it")
            val wishlistRow = searchBody.select(s"#wishlist-list td a").headOption
            wishlistRow.value.text shouldBe "Some third wishlist"

            val wishlistRow2 = searchBody.select(s"#wishlist-list td:eq(1) a").headOption
            wishlistRow2 shouldBe defined
            wishlistRow2.value.text shouldBe "testerson3"
         }
      }

   }
}
