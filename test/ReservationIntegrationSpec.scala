package com.flurdy.wishlist

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.OptionValues._
import play.api.libs.ws.WSResponse

trait ReservationIntegrationHelper extends IntegrationHelper {

}


class ReservationIntegrationSpec extends AsyncFeatureSpec
      with GivenWhenThen with ScalaFutures with Matchers
      with IntegrationPatience with StartAndStopServer
      with RegistrationIntegrationHelper
      with LoginIntegrationHelper
      with CookieIntegrationHelper
      with WishlistIntegrationHelper {

   info("As a wish giver")
   info("I want to reserve wishes")
   info("so that I can give them to the recipient")

   feature("Reserve flow") {

      scenario("find a wish without logging in") (pending)
      scenario("find a wish with logging in") (pending)
      scenario("reserve a wish") (pending)
      scenario("unreserve a wish") (pending)
      
   }
}
