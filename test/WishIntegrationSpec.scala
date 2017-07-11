package com.flurdy.wishlist

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.OptionValues._
import play.api.libs.ws.WSResponse

trait WishIntegrationHelper extends IntegrationHelper {

}

class WishIntegrationSpec extends AsyncFeatureSpec
      with GivenWhenThen with ScalaFutures with Matchers
      with IntegrationPatience with StartAndStopServer
      with RegistrationIntegrationHelper
      with LoginIntegrationHelper
      with CookieIntegrationHelper
      with WishlistIntegrationHelper {

   info("As a wish recipient")
   info("I want manage a wish")
   info("so that I can ensure my wishes are informative")

   feature("Wish flow") {

      scenario("Update wish")(pending)
      scenario("Move a wish")(pending)
      scenario("Delete a wish")(pending)
   }

   feature("Wish links") {
      scenario("Add link to wish")(pending)
      scenario("Remove link from wish")(pending)

   }
}
