package com.flurdy.wishlist

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.OptionValues._
import org.scalatestplus.play._
import play.api.http._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._

import scala.concurrent.Await._
import scala.concurrent.{ExecutionContext, Future}


trait RegistrationIntegrationHelper extends IntegrationHelper {

   val registerUrl = s"$baseUrl/register"

   def register(username: String) = {
      val registerFormData = Map(
         "fullname" -> Seq("Test Testerson"),
         "username" -> Seq(username),
         "email"    -> Seq(s"${username}@example.com"),
         "password" -> Seq("simsalabim"),
         "confirm"  -> Seq("simsalabim")
      )
      getWsClient().url(registerUrl).withFollowRedirects(false).post(registerFormData)
   }

   def findVerificationUrl(username: String)(implicit ec: ExecutionContext) = {
      val hashUrl = s"$baseUrl/test-only/recipient/${username.toLowerCase().trim}/verify/find"
      getWsClient().url(hashUrl).withFollowRedirects(false).get().map { response =>
         response.header("Location").headOption
      }
   }

   def verify(verifyUrl: String) =
      getWsClient().url(s"$baseUrl$verifyUrl").withFollowRedirects(false).get()

}

class WishRegistrationIntegrationSpec extends AsyncFeatureSpec
   with GivenWhenThen with ScalaFutures with Matchers
   with IntegrationPatience with StartAndStopServer
   with RegistrationIntegrationHelper with CookieIntegrationHelper {

   info("As a wish recipient")
   info("I want to register with the Wish application")
   info("so that I can list my wishes")

   feature("Registration flow with verification disabled") {

      scenario("Front page has registration form") {

         Given("the front page")
         val indexUrl = s"$baseUrl/"

         When("requested")
         val response = getWsClient().url(indexUrl).get()

         Then("response should have a brief registration form")
         response map { r =>
            r.status shouldBe 200
            val elements = ScalaSoup.parse(r.body).select("#register-box form input[name=email]")
            elements should not be empty
         }
      }

      scenario("Register form redirect") {

         Given("A valid unused username")
         val uniqueUsername = "Testerson"

         When("submiting the brief registration form")
         val registerUrl = s"$baseUrl/register.html?email=$uniqueUsername"
         val response = getWsClient().url(registerUrl).get()

         Then("should redirect and display the full registration form")
         response map { r =>
            r.status shouldBe 200
            val registerDocument = ScalaSoup.parse(r.body)
            val h2 = registerDocument.select("#register-page h2").headOption
            h2.value.text shouldBe "Register"

            And("prefill the username field")
            val usernameField = registerDocument.select("form input[name=username]").headOption
            usernameField.value.attr("value") shouldBe uniqueUsername
            val emailField = registerDocument.select("form input[name=email]").headOption
            emailField.value.attr("value") shouldBe uniqueUsername
         }
      }

      scenario("Submit full registration form") {
         val registerUrl = s"$baseUrl/register/"

         Given("a filled in registration form")
         getWsClient().url(registerUrl).get() map { r =>
            r.status shouldBe 200
            val h2 = ScalaSoup.parse(r.body).select("#register-page h2").headOption
            h2.value.text shouldBe "Register"
         }

         And("email verification is disabled")

         When("submitting the registration form")
         val response = register("Testerson")

         Then("should be redirect to front login form")
         response map { r =>
            r.status shouldBe 303
            r.header("Location").headOption.value shouldBe "/"
            findFlashCookie(r).value shouldBe "messageSuccess=Welcome%2C+you+have+successfully+registered"
         }
      }

      scenario("Unregister") (pending)

   }
}

class WishRegistrationWithVerificationIntegrationSpec extends AsyncFeatureSpec
   with GivenWhenThen with ScalaFutures with Matchers
   with IntegrationPatience with StartAndStopServer with LoginIntegrationHelper
   with RegistrationIntegrationHelper with CookieIntegrationHelper {

   applicationConfiguration = Map("play.http.router" -> "testonly.Routes",
                                  "com.flurdy.wishlist.feature.email.verification.enabled" -> true)

   feature("Registration flow with verification enabled") {

      scenario("Submit full registration form") {
         val registerUrl = s"$baseUrl/register/"

         Given("a filled in registration form")
         getWsClient().url(registerUrl).get() map { r =>
            r.status shouldBe 200
            val h2 = ScalaSoup.parse(r.body).select("#register-page h2").headOption
            h2.value.text shouldBe "Register"
         }

         And("email verification is enabled")

         When("submitting the registration form")
         val response = register("AnotherTesterson")

         Then("should be redirect to front login form")
         response map { r =>
            r.status shouldBe 303
            r.header("Location").headOption.value shouldBe "/"
            val flash = findFlashCookie(r).value
            flash should startWith ("messageSuccess=Welcome%2C+you+have+successfully+registered")
            flash should endWith ("sent+to+you")
         }
      }

      scenario("unable to log in unless verified"){
         val flow = for {
            registerResponse <- register("Testerson99")
            loginResponse    <- login("Testerson99")
         } yield (registerResponse, loginResponse)

         flow map { case (registerResponse, loginResponse) =>
            Given("email verification is enabled")

            And("a pending registration")
            registerResponse.status shouldBe 303
            registerResponse.header("Location").headOption.value shouldBe "/"

            When("trying to log in")
            loginResponse.status shouldBe 400

            Then("should not be able to")
            findFlashCookie(loginResponse).value shouldBe "messageError=Log+in+failed.+Email+not+verified.+Please+check+your+email"
         }
      }

      scenario("verifying email post registration"){
         val flow = for {
            registerResponse <- register("Testerson999")
            loginResponse1   <- login("Testerson999")
            verificationUrl  <- findVerificationUrl("Testerson999")
            verifyResponse   <- verificationUrl.fold(throw new IllegalStateException("no hash found"))( v => verify(v))
            loginResponse2   <- login("Testerson999")
         } yield (registerResponse, loginResponse1, verifyResponse, loginResponse2)

         flow map { case (registerResponse, loginResponse1, verifyResponse, loginResponse2) =>

            Given("email verification is enabled")

            And("a pending registration")
            registerResponse.status shouldBe 303
            registerResponse.header("Location").headOption.value shouldBe "/"
            loginResponse1.status shouldBe 400
            findFlashCookie(loginResponse1).value shouldBe "messageError=Log+in+failed.+Email+not+verified.+Please+check+your+email"

            When("clicking on verify email link sent")
            verifyResponse.status shouldBe 303

            Then("registration should be verified")
            val flash = findFlashCookie(verifyResponse).value
            flash should startWith ("messageSuccess=Email+address+verified.+Please+log+in")

            And("able to log in")
            loginResponse2.status shouldBe 303
            findFlashCookie(loginResponse2).value shouldBe "message=You+have+logged+in"
         }
      }
   }
}
