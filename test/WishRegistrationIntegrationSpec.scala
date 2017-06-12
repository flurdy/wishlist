package com.flurdy.wishlist

// import org.jsoup.Jsoup
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.OptionValues._
import org.scalatestplus.play._
import play.api.http._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._
import scala.concurrent.Await._


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
}

class WishRegistrationIntegrationSpec extends AsyncFeatureSpec
   with GivenWhenThen with ScalaFutures with Matchers
   with IntegrationPatience with StartAndStopServer
   with RegistrationIntegrationHelper {

   info("As a wish recipient")
   info("I want to register with the Wish application")
   info("so that I can list my wishes")

   feature("Registration flow") {

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

         When("submitting the registration form")
         val response = register("Testerson")

         Then("should be redirect to front login form")
         response map { r =>
            r.status shouldBe 303
            r.header("Location").headOption.value shouldBe "/"
         }
      }

      scenario("Unregister") (pending)

   }
}
