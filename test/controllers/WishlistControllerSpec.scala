package controllers

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Future
import com.flurdy.wishlist.ScalaSoup
import models._
import repositories._


class WishlistControllerSpec extends BaseControllerSpec {

   trait Setup extends WithMock {
      val appConfigMock = mock[ApplicationConfig]
      val recipientRepositoryMock = mock[RecipientRepository]
      val recipientLookupMock = mock[RecipientLookup]
      val wishlistRepositoryMock = mock[WishlistRepository]
      val wishlistLookupMock = mock[WishlistLookup]
      val wishlistOrganiserRepositoryMock = mock[WishlistOrganiserRepository]
      val wishLookupMock = mock[WishLookup]
      val wishRepositoryMock = mock[WishRepository]
      val wishEntryRepositoryMock = mock[WishEntryRepository]
      val wishLinkRepositoryMock = mock[WishLinkRepository]
      val reservationRepositoryMock = mock[ReservationRepository]
      val featureTogglesMock = mock[FeatureToggles]

      val application = new GuiceApplicationBuilder()
            .overrides(bind[RecipientLookup].toInstance(recipientLookupMock))
            .build()

      val controller = new WishlistController(
         controllerComponents, appConfigMock, 
         usernameAction, maybeCurrentRecipientAction)(
         executionContext, wishlistOrganiserRepositoryMock, 
         wishlistRepositoryMock, wishlistLookupMock,
         wishLinkRepositoryMock, wishEntryRepositoryMock,
         wishRepositoryMock, wishLookupMock,
         recipientLookupMock, recipientRepositoryMock,
         reservationRepositoryMock, featureTogglesMock)
      
      when(appConfigMock.findString(anyString)).thenReturn(None)
   }

   trait WishlistSetup extends Setup {

      val recipient = new Recipient(222).copy(username = "someuser")
      val another   = new Recipient(444).copy(username = "somerecipient")
      val wishlist  = new Wishlist(123, recipient).copy( title = "Some wishlist",
                                                         description = Some("Blah blah") )
      val wish      = new Wish(444, recipient)
      val wishes    = List(wish)

      when( wishlistLookupMock.findWishlist(123) )
            .thenReturn(Future.successful(Some(wishlist)))
      when( recipientLookupMock.findRecipient("someuser") )
            .thenReturn( Future.successful( Some( recipient ) ))
      when( recipientLookupMock.findRecipient("somerecipient") )
            .thenReturn( Future.successful( Some( another ) ))
      when( recipientRepositoryMock.findRecipientById(222) )
            .thenReturn( Future.successful( Some( recipient) ))
      when( recipientRepositoryMock.findRecipientById(444) )
            .thenReturn( Future.successful( Some( another ) ))
      when( wishLookupMock.findWishes(wishlist)(wishLinkRepositoryMock, executionContext) )
            .thenReturn( Future.successful(wishes) )

      def showAnonymousWishlist() = {

         val result = controller.showWishlist("someuser", 123).apply(FakeRequest())

         status(result) mustBe 200
         ScalaSoup.parse(contentAsString(result))

      }

      def showRecipientWishlist() = {

         when( wishlistLookupMock.isOrganiserOfWishlist( recipient, wishlist) )
               .thenReturn( Future.successful( true ))
         when( wishlistRepositoryMock.findRecipientWishlists( recipient ) )
               .thenReturn( Future.successful( List(wishlist) ))
         when( wishlistRepositoryMock.findOrganisedWishlists( recipient ) )
               .thenReturn( Future.successful( List() ))

         val result = controller.showWishlist("someuser", 123).apply(
            FakeRequest().withSession("username"  -> "someuser"))

         status(result) mustBe 200
         ScalaSoup.parse(contentAsString(result))

      }

   }

   "Wishlist controller" must given {
      "not logged in" when requesting {
         "[GET] /recipient/someuser/wishlist/122/" should show {
            "wishlist page" in new WishlistSetup {
               showAnonymousWishlist()
                     .select("#view-wishlist-page")
                     .headOption mustBe defined
            }
            "title" in new WishlistSetup {
               showAnonymousWishlist()
                     .select("#view-wishlist-page h3")
                     .headOption.value.text mustBe "Some wishlist"
            }
            "recipient" in new WishlistSetup {
               showAnonymousWishlist()
                     .select("#view-wishlist-page p a")
                     .headOption.value.text mustBe "someuser"
            }
            "description" in new WishlistSetup {
               showAnonymousWishlist()
                     .select("#view-wishlist-page p:eq(2)")
                     .headOption.value.text mustBe "Blah blah"
            }
            "wishes" in new WishlistSetup {
               showAnonymousWishlist()
                     .select("#view-wishlist-page #wishlist-wishes")
                     .headOption mustBe defined
            }
            "wish" in new WishlistSetup {
               showAnonymousWishlist()
                     .select("#view-wishlist-page #wishlist-wishes ul #wish-row-444")
                     .headOption mustBe defined
            }
         }
      }
   }

   "logged in as someuser" when requesting {
      "[GET] /recipient/someuser/wishlist/122/" should show {
         "editable wishlist page" in new WishlistSetup {
            showRecipientWishlist()
                  .select("#view-wishlist-page > .well > .control-group .buttons a")
                  .headOption.value.text mustBe "edit wishlist"
         }
      }
   }
}
