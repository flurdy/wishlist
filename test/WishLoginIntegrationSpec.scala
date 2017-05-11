package com.flurdy.wishlist

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.OptionValues._
import org.scalatestplus.play._
import play.api.http._
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._
import scala.concurrent.Await._

trait LoginIntegrationHelper extends IntegrationHelper {

   val loginUrl  = s"$baseUrl/login/"
   val logoutUrl = s"$baseUrl/logout"

   def login(username: String) = {
      val loginFormData = Map(
         "username" -> Seq(username),
         "password" -> Seq("simsalabim")
      )
      getWsClient().url(loginUrl).withFollowRedirects(false).post(loginFormData)
   }

   def logout() = getWsClient().url(logoutUrl).withFollowRedirects(false).get()
}

class WishLoginIntegrationSpec extends AsyncFeatureSpec
   with GivenWhenThen with ScalaFutures with Matchers
   with IntegrationPatience with StartAndStopServer
   with RegistrationIntegrationHelper
   with LoginIntegrationHelper {

   info("As a wish recipient")
   info("I want to login to the Wish application")
   info("so that I can list my wishes")

   feature("Login flow") {

      val frontUrl = s"$baseUrl/"

      scenario("Registration and log in") {

         val flow = for {
            _           <- register("Testerson")
            frontBefore <- getWsClient().url(frontUrl).get()
            login       <- login("Testerson")
            frontAfter  <- getWsClient().url(frontUrl).get()
         } yield(frontBefore, login, frontAfter)

         flow map { case(frontBefore, login, frontAfter) =>
            Given("Registered as a member of Wish")
            And("not logged in")
            val loginFormBefore = ScalaSoup.parse(frontBefore.body).select("#login-box input").headOption
            loginFormBefore shouldBe defined

            When("submitting the login form")
            login.status shouldBe 303
            login.header("Location").headOption.value shouldBe "/"

            Then("should be logged in")
            val loginFormAfter = ScalaSoup.parse(frontAfter.body).select("#login-box input").headOption
            loginFormAfter shouldBe None
            val logoutLink = ScalaSoup.parse(frontAfter.body).select("#logout-box li a").headOption
            logoutLink.value.text shouldBe "Testerson"
         }
      }

      scenario("Can log out"){

         val flow = for {
            _              <- register("Testerson")
            _              <- login("Testerson")
            frontLogin    <- getWsClient().url(frontUrl).get()
            logoutResponse <- logout()
            frontLogout     <- getWsClient().url(frontUrl).get()
         }  yield(frontLogin, logoutResponse, frontLogout)

         flow map { case(frontLogin, logoutResponse, frontLogout) =>
            Given("a logged in user")
            val logoutLink = ScalaSoup.parse(frontLogin.body).select("#logout-box li a").headOption
            logoutLink.value.text shouldBe "Testerson"

            When("logging out")
            logoutResponse.status shouldBe 303
            logoutResponse.header("Location").headOption.value shouldBe "/"

            Then("should be logged out")
            val loggedOutLink = ScalaSoup.parse(frontLogout.body).select("#logout-box li a").headOption
            loggedOutLink.value.text shouldBe "log in"
         }
      }

      scenario("Logged in, out and in again"){
         val flow = for {
            _              <- register("Testerson")
            _              <- login("Testerson")
            frontBefore    <- getWsClient().url(frontUrl).get()
            _              <- logout()
            frontLogout    <- getWsClient().url(frontUrl).get()
            loginResponse  <- login("Testerson")
            frontAfter     <- getWsClient().url(frontUrl).get()
         } yield (frontBefore, frontLogout, loginResponse, frontAfter)

         flow map { case(frontBefore, frontLogout, loginResponse, frontAfter) =>

            Given("a logged in user")
            val logoutLink = ScalaSoup.parse(frontBefore.body).select("#logout-box li a").headOption
            logoutLink.value.text shouldBe "Testerson"

            When("logging out")
            val loggedOutLink = ScalaSoup.parse(frontLogout.body).select("#logout-box li a").headOption
            loggedOutLink.value.text shouldBe "log in"

            And("logging in again")
            loginResponse.status shouldBe 303
            loginResponse.header("Location").headOption.value shouldBe "/"

            Then("should be logged in")
            val logout2Link = ScalaSoup.parse(frontAfter.body).select("#logout-box li a").headOption
            logout2Link.value.text shouldBe "Testerson"

         }
      }
   }
}
