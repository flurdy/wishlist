package controllers

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Future
import com.flurdy.wishlist.ScalaSoup
import models._
import repositories._


class WishControllerSpec extends BaseUnitSpec with Results with GuiceOneAppPerSuite {

   trait Setup {
      val configurationMock = mock[Configuration]
      val recipientRepositoryMock = mock[RecipientRepository]
      val recipientLookupMock = mock[RecipientLookup]
      val wishlistRepositoryMock = mock[WishlistRepository]
      val wishlistLookupMock = mock[WishlistLookup]
      val wishLookupMock = mock[WishLookup]
      val wishRepositoryMock = mock[WishRepository]
      val wishEntryRepositoryMock = mock[WishEntryRepository]
      val reservationRepositoryMock = mock[ReservationRepository]
      val wishController = new WishController(configurationMock, recipientLookupMock)(
         wishlistRepositoryMock, wishRepositoryMock, wishEntryRepositoryMock, wishlistLookupMock, wishLookupMock, reservationRepositoryMock, recipientRepositoryMock)
      val wishlistController = new WishlistController(configurationMock, recipientLookupMock)(
         wishlistRepositoryMock, wishRepositoryMock, wishEntryRepositoryMock, wishlistLookupMock, wishLookupMock, reservationRepositoryMock, recipientRepositoryMock)
   }

   trait WishSetup extends Setup {

      val recipient = new Recipient(222).copy(username = "someuser")
      val another   = new Recipient(666).copy(username = "someotheruser")
      val otheruser = new Recipient(777).copy(username = "somethirduser")
      val unreservedWish = new Wish(444, recipient)
      val anotherWish    = new Wish(544, recipient)
      val otherWish      = new Wish(644, recipient)
      val wishlist  = new Wishlist(123, recipient).copy( title = "Some wishlist",
                                                         description = Some("Blah blah") )
      val anotherReservation = Reservation( Some(888), another, anotherWish)
      val otherReservation   = Reservation( Some(888), otheruser, otherWish)
      val anotherReservedWish = anotherWish.copy( reservation = Some(anotherReservation))
      val otherReservedWish   = otherWish.copy( reservation = Some(otherReservation))
      val wishes    = List(unreservedWish, anotherReservedWish, otherReservedWish)
      val updatedWish = unreservedWish.copy( title = "updated wishlist",
                                            description = Some("updated description") )

      when( wishLookupMock.findWishById(444) )
            .thenReturn(Future.successful(Some(unreservedWish)))
      when( wishLookupMock.findWishById(544) )
            .thenReturn(Future.successful(Some(anotherReservedWish)))
      when( wishLookupMock.findWishById(644) )
            .thenReturn(Future.successful(Some(otherReservedWish)))

      when( wishlistLookupMock.findWishlist(123) )
            .thenReturn(Future.successful(Some(wishlist)))
      when( wishLookupMock.findWishes(wishlist) )
            .thenReturn( Future.successful(wishes) )
      when( recipientLookupMock.findRecipient("someuser") )
            .thenReturn( Future.successful( Some( recipient ) ))
      when( recipientLookupMock.findRecipient("someotheruser") )
            .thenReturn( Future.successful( Some( another ) ))
      when( recipientLookupMock.findRecipient("somethirduser") )
            .thenReturn( Future.successful( Some( otheruser ) ))
      when( recipientRepositoryMock.findRecipientById(222) )
            .thenReturn( Future.successful( Some( recipient ) ))
      when( wishlistRepositoryMock.findRecipientWishlists( recipient ) )
            .thenReturn( Future.successful( List(wishlist) ))
      when( wishlistRepositoryMock.findOrganisedWishlists( recipient ) )
            .thenReturn( Future.successful( List() ))

      when( wishlistLookupMock.isOrganiserOfWishlist(another, wishlist))
            .thenReturn( Future.successful( true ))
      when( wishlistLookupMock.isOrganiserOfWishlist(otheruser, wishlist))
            .thenReturn( Future.successful( false ))

      when( wishRepositoryMock.updateWish(updatedWish))
            .thenReturn( Future.successful(updatedWish))

      def showSessionWishlist(sessionUsername: String) = {
         val result = wishlistController.showWishlist("someuser", 123)
                              .apply(FakeRequest().withSession("username"  -> sessionUsername))
         status(result) mustBe 200
         ScalaSoup.parse(contentAsString(result))
      }

      def reserveWish(sessionUsername: String) =
         wishController.reserveWish("someuser", 123, 444)
                       .apply(FakeRequest().withSession("username"  -> sessionUsername))

      def updateSessionWish(sessionUsername: String) =
         wishController.updateWish("someuser", 123, 444)
               .apply(FakeRequest()
                     .withSession("username" -> sessionUsername)
                     .withFormUrlEncodedBody(
                        "title" -> "updated wishlist",
                        "description" -> "updated description"))

      def deleteWish(sessionUsername: String) =
         wishController.removeWishFromWishlist("someuser", 123, 444)
                       .apply(FakeRequest().withSession("username"  -> sessionUsername))
   }

   trait WishRecipientSetup extends WishSetup {
      def showWishlist() = showSessionWishlist("someuser")
      def updateWish()   = updateSessionWish("someuser")
   }

   trait WishOrganiserSetup extends WishSetup {

      when( wishlistRepositoryMock.findRecipientWishlists( another ) )
            .thenReturn( Future.successful( List() ))
      when( wishlistRepositoryMock.findOrganisedWishlists( another ) )
            .thenReturn( Future.successful( List(wishlist) ))

      clearInvocations(reservationRepositoryMock)
      when( reservationRepositoryMock.saveReservation( any[Reservation]))
            .thenReturn( Future.successful( anotherReservation ))

      def showWishlist() = showSessionWishlist("someotheruser")
      def updateWish()   = updateSessionWish("someotheruser")
   }

   trait WishOtherSetup extends WishSetup {
      clearInvocations(reservationRepositoryMock)
      when( reservationRepositoryMock.saveReservation( any[Reservation]))
            .thenReturn( Future.successful( otherReservation ))

      def showWishlist() = showSessionWishlist("somethirduser")
      def updateWish()   = updateSessionWish("somethirduser")
   }

   trait WishAnonSetup extends WishSetup {
      def showWishlist() = {
         val result = wishlistController.showWishlist("someuser", 123).apply(FakeRequest())
         status(result) mustBe 200
         ScalaSoup.parse(contentAsString(result))
      }
      def reserveWish() = wishController.reserveWish("someuser", 123, 444).apply(FakeRequest())

      def updateWish() = wishController.updateWish("someuser", 123, 444)
            .apply(FakeRequest().withFormUrlEncodedBody())

      def deleteWish() = wishController.removeWishFromWishlist("someuser", 123, 444).apply(FakeRequest())
   }

   "Wish controller" when {

      "logged in as wishlist recipient" should {
         "not see reserve button" in new WishRecipientSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-444 .reserve-button")
                  .headOption mustBe None
         }
         "not see reserve status" in new WishRecipientSetup {
            showWishlist()
                  .select("#view-wishlist-page ul #wish-row-544 .reserved")
                  .headOption mustBe None
         }
         "not be able to reserve its own wish" in new WishRecipientSetup {
            status(reserveWish("someuser")) mustBe 401
         }
         "see edit button" in new WishRecipientSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-444 .wish-edit-button")
                  .headOption mustBe defined
         }
         "be able to update wish" in new WishRecipientSetup {
            status(updateWish()) mustBe 303

            verify( wishRepositoryMock ).updateWish(updatedWish)
         }
         "be able to delete wish" in new WishRecipientSetup {
//            status(deleteWish()) mustBe 303
            pending
         }
      }

      "logged in as organiser of wishlist" should {
         "see edit button" in new WishOrganiserSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-444 .wish-edit-button")
                  .headOption mustBe defined
         }
         "see reserve status if reserved" in new WishOrganiserSetup {
            showWishlist()
                  .select("#view-wishlist-page ul #wish-row-544 .reserved")
                  .headOption.value.text mustBe "reserved"
         }
         "see reserve button" in new WishOrganiserSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-444 .reserve-button")
                  .headOption mustBe defined
         }
         "see reserve cancel button if reserved" in new WishOrganiserSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-544 .reserve-button")
                  .headOption.value.text mustBe "cancel reservation"
         }
         "not see reserve cancel button if reserved by someone else" in new WishOrganiserSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-644 .reserve-button")
                  .headOption mustBe None
         }
         "be able to reserve wish" in new WishOrganiserSetup {
            status(reserveWish("someotheruser")) mustBe 303
         }
         "be able to update wish" in new WishOrganiserSetup {
            status(updateWish()) mustBe 303
         }
         "be able to delete wish" is (pending)
      }

      "logged in but not recipient nor organiser of wishlist" should {
         "not see edit button" in new WishOtherSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-444 .wish-edit-button")
                  .headOption mustBe None
         }
         "see reserve status if reserved" in new WishOtherSetup {
            showWishlist()
                  .select("#view-wishlist-page ul #wish-row-544 .reserved")
                  .headOption.value.text mustBe "reserved"
         }
         "see reserve button" in new WishOtherSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-444 .reserve-button")
                  .headOption mustBe defined
         }
         "be able to reserve wish" in new WishOtherSetup {
            status(reserveWish("somethirduser")) mustBe 303
         }
         "not be able to update wish" in new WishOtherSetup {
            status(updateWish()) mustBe 401
         }
         "not be able to delete wish" is (pending)
      }

      "not logged in" should {
         "not see edit button" in new WishAnonSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-444 .wish-edit-button")
                  .headOption mustBe None
         }
         "not see reserve status if reserved" in new WishAnonSetup {
            showWishlist()
                  .select("#view-wishlist-page ul #wish-row-544 .reserved")
                  .headOption mustBe None
         }
         "see reserve button irrespective of status" in new WishAnonSetup {
            showWishlist()
                  .select("#view-wishlist-page #wishModal-444 .reserve-button")
                  .headOption mustBe defined
         }
         "not be able to reserve wish" in new WishAnonSetup {
            status(reserveWish()) mustBe 401
         }
         "not be able to update wish" in new WishAnonSetup {
            status(updateWish()) mustBe 401
         }
         "not be able to delete wish" is (pending)
      }
   }
}
