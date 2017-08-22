package controllers

import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Future
import com.flurdy.wishlist.ScalaSoup
import models._
import repositories._


class RecipientControllerSpec extends BaseUnitSpec with Results with GuiceOneAppPerSuite {

   trait Setup {
      val configurationMock = mock[Configuration]
      val recipientRepositoryMock = mock[RecipientRepository]
      val recipientLookupMock = mock[RecipientLookup]
      val wishlistRepositoryMock = mock[WishlistRepository]
      val reservationRepositoryMock = mock[ReservationRepository]
      val controller = new RecipientController(configurationMock, recipientLookupMock)(recipientRepositoryMock, wishlistRepositoryMock, reservationRepositoryMock)

      val recipient = new Recipient("someuser").copy(recipientId = Some(1222), fullname = Some("Some User"))
      val anotherRecipient = new Recipient("someother").copy(recipientId = Some(5555), fullname = Some("Some Other"))

      when( recipientLookupMock.findRecipient("someuser") )
            .thenReturn( Future.successful( Some( recipient ) ) )
      when( recipientLookupMock.findRecipient("someother") )
            .thenReturn( Future.successful( Some( anotherRecipient ) ) )
      when( recipientRepositoryMock.findRecipientById(1222) )
            .thenReturn( Future.successful( Some( recipient ) ) )
      when( recipientRepositoryMock.findRecipientById(5555) )
            .thenReturn( Future.successful( Some( anotherRecipient ) ) )
   }

   trait ProfileSetup extends Setup {

      val wishlist  = new Wishlist("Some wishlist", recipient).copy(wishlistId = Some(123))
      val organised = new Wishlist("Some organised wishlist", anotherRecipient).copy(wishlistId = Some(123))
      val wish = new Wish( 222, "Some wish title", anotherRecipient)
      val reserved = Reservation( Some(111), recipient, wish)
      val reservations = List(reserved)

      when( wishlistRepositoryMock.findRecipientWishlists(recipient) )
            .thenReturn( Future.successful( List(wishlist) ))

      def showAnonymousProfile() = {

         val result = controller.showProfile("someuser").apply(FakeRequest())

         status(result) mustBe 200
         ScalaSoup.parse(contentAsString(result))

      }

      def showAnotherRecipientProfile() = {

         when( wishlistRepositoryMock.findRecipientWishlists(anotherRecipient) )
               .thenReturn( Future.successful( List(organised) ))

         val result = controller.showProfile("someother").apply(
               FakeRequest().withSession("username"  -> "someuser"))

         status(result) mustBe 200
         ScalaSoup.parse(contentAsString(result))

      }

      def showRecipientProfile() = {

         when( wishlistRepositoryMock.findOrganisedWishlists(recipient) )
               .thenReturn( Future.successful( List(organised) ))
         when( reservationRepositoryMock.findReservationsByReserver(recipient) )
               .thenReturn( Future.successful( reservations))
         when( reservationRepositoryMock.inflateReservationsReserver(reservations)(recipientRepositoryMock) )
               .thenReturn( Future.successful( reservations ))
         when( reservationRepositoryMock.inflateReservationsWishRecipient(reservations)(recipientRepositoryMock) )
               .thenReturn( Future.successful( reservations ))

         val result = controller.showProfile("someuser").apply(
               FakeRequest().withSession("username"  -> "someuser"))

         status(result) mustBe 200
         ScalaSoup.parse(contentAsString(result))

      }

   }

   trait EditProfileSetup extends Setup {

      def showEditProfile(username: String) = {

         val result = controller.showEditRecipient(username).apply(
               FakeRequest().withSession("username"  -> "someuser"))

         status(result) mustBe 200
         ScalaSoup.parse(contentAsString(result))

      }
   }

   "Recipient controller" when requesting {

      "[GET] /recipient/someuser/" should {
         "show profile page" in new ProfileSetup {

            showAnonymousProfile().select("#profile-page").headOption mustBe defined

            verify( recipientLookupMock ).findRecipient("someuser")
         }

         "show a profile given not logged in" which has {

            "a username" in new ProfileSetup {
               showAnonymousProfile()
                     .select("#profile-page h4:eq(0)")
                     .headOption.value.text mustBe "Recipient: someuser"
            }

            "a fullname" in new ProfileSetup {
               showAnonymousProfile()
                     .select("#profile-page h4:eq(1)")
                     .headOption.value.text mustBe "Full name: Some User"
            }
            "no edit profile button" in new ProfileSetup {
               showAnonymousProfile()
                     .select("#profile-page .control-group a")
                     .headOption mustBe None

            }

            "wishlists" which has {
               "a title" in new ProfileSetup {
                  showAnonymousProfile()
                        .select("#profile-page #wishlist-list tr td a")
                        .headOption.value.text mustBe "Some wishlist"
               }
               "a valid url" in new ProfileSetup {
                  showAnonymousProfile()
                        .select("#profile-page #wishlist-list tr td a")
                        .headOption.value.attr("href") mustBe "/someuser/wishlist/123/"
               }
            }
         }

         "show a profile given logged in as another recipient" which has {
            "no edit profile button" in new ProfileSetup {
               showAnotherRecipientProfile()
                     .select("#profile-page .control-group a")
                     .headOption mustBe None

            }
            "no organised wishlists" in new ProfileSetup {
               showAnotherRecipientProfile()
                     .select("#profile-page #organised-list")
                     .headOption mustBe None
            }
         }

         "show a profile given logged in as profile recipient" which has {

            "organised wishlists" in new ProfileSetup {
               showRecipientProfile()
                     .select("#profile-page #organised-list tr td:eq(0)")
                     .headOption.value.text mustBe "Some organised wishlist"
            }

            "reserved wishes" in new ProfileSetup {
               val profile = showRecipientProfile()
               profile.select("#profile-page #reservation-list tr:eq(1) td")
                     .headOption.value.text mustBe "Some wish title"
               profile.select("#profile-page #reservation-list tr:eq(1) td:eq(1) a")
                     .headOption.value.text mustBe "someother"

               verify( reservationRepositoryMock ).findReservationsByReserver(recipient)
               verify( reservationRepositoryMock ).inflateReservationsWishRecipient(reservations)(recipientRepositoryMock)
               verify( reservationRepositoryMock ).inflateReservationsReserver(reservations)(recipientRepositoryMock)
            }
            "create wishlist button" in new ProfileSetup {
               showRecipientProfile()
                     .select("#profile-page button")
                     .headOption.value.text mustBe "create"

            }
            "edit profile button" in new ProfileSetup {
               showRecipientProfile()
                     .select("#profile-page .control-group a")
                     .headOption.value.text mustBe "edit recipient"

            }
         }
      }

      "[GET] /recipient/someuser/" should {
         "show edit recipient page" in new EditProfileSetup {

            showEditProfile("someother").select("#edit-profile-page").headOption mustBe defined

            verify( recipientLookupMock ).findRecipient("someother")
         }
      }
   }
}
