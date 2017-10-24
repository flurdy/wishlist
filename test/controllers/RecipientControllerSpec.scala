package controllers

import com.github.t3hnar.bcrypt._
import org.mockito.ArgumentMatchers.{any,anyString,eq=>eqTo}
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
import notifiers._


class RecipientControllerSpec extends BaseUnitSpec with Results with GuiceOneAppPerSuite {

   trait Setup {
      val configurationMock = mock[Configuration]
      val appConfigMock = mock[ApplicationConfig]
      val recipientRepositoryMock = mock[RecipientRepository]
      val recipientLookupMock = mock[RecipientLookup]
      val wishlistRepositoryMock = mock[WishlistRepository]
      val wishLinkRepositoryMock = mock[WishLinkRepository]
      val wishLookupMock = mock[WishLookup]
      val wishRepositoryMock = mock[WishRepository]
      val wishEntryRepositoryMock = mock[WishEntryRepository]
      val wishlistOrganiserRepositoryMock = mock[WishlistOrganiserRepository]
      val reservationRepositoryMock = mock[ReservationRepository]
      val featureTogglesMock = mock[FeatureToggles]
      val emailNotifierMock = mock[EmailNotifier]
      val controller = new RecipientController(configurationMock, recipientLookupMock, emailNotifierMock, appConfigMock)(
            recipientRepositoryMock,
            wishlistRepositoryMock,
            wishLinkRepositoryMock,
            wishLookupMock,
            wishRepositoryMock,
            wishEntryRepositoryMock,
            wishlistOrganiserRepositoryMock,
            reservationRepositoryMock,
            featureTogglesMock)

      val recipient = new Recipient("someuser")
                        .copy(recipientId = Some(1222),
                              fullname = Some("Some User"),
                              email = "someuser@example.com")
      val anotherRecipient = new Recipient("someother")
                        .copy(recipientId = Some(5555),
                              fullname = Some("Some Other"),
                              email = "someother@example.com" )

      when( recipientLookupMock.findRecipient("someuser") )
            .thenReturn( Future.successful( Some( recipient ) ) )
      when( recipientLookupMock.findRecipient("someother") )
            .thenReturn( Future.successful( Some( anotherRecipient ) ) )
      when( recipientLookupMock.findRecipient("someunknown") )
            .thenReturn( Future.successful( None ) )
      when( recipientRepositoryMock.findRecipientById(1222) )
            .thenReturn( Future.successful( Some( recipient ) ) )
      when( recipientRepositoryMock.findRecipientById(5555) )
            .thenReturn( Future.successful( Some( anotherRecipient ) ) )
      when(appConfigMock.getString(anyString)).thenReturn(None)
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


         (status(result), ScalaSoup.parse(contentAsString(result)))

      }
   }

   trait UpdateProfileSetup extends Setup {
      val updatedRecipient = new Recipient("someuser")
               .copy(recipientId = Some(1222),
                     fullname = Some("some new name"),
                     email = "some-new-email@example.com")

      def updateProfile(username: String) = {
         controller.updateRecipient(username).apply(
            FakeRequest()
               .withSession("username"  -> "someuser")
               .withFormUrlEncodedBody(
                  "oldusername" -> "someuser",
                  "username" -> "someuser",
                  "fullname" -> "some new name",
                  "email"    -> "some-new-email@example.com"))
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

      "[GET] /recipient/someuser/edit.html" when given {
         "a valid username as current recipient" should {
            "show edit recipient page" in new EditProfileSetup {

               val statusAndBody = showEditProfile("someuser")

               statusAndBody._1 mustBe 200
               statusAndBody._2.select("#edit-profile-page").headOption mustBe defined

               verify( recipientLookupMock, times(2) ).findRecipient("someuser")
            }
         }
         "a valid other username" should {
            "show not authorized page" in new EditProfileSetup {

               showEditProfile("someother")._1 mustBe 401

               verify( recipientLookupMock ).findRecipient("someother")
            }
         }
         "a unknown username" should {
            "show not found page" in new EditProfileSetup {

               showEditProfile("someunknown")._1 mustBe 404

               verify( recipientLookupMock ).findRecipient("someunknown")
            }
         }
      }

      "[PUT] /recipient/someuser/update" when given {
        "a valid username as current recipient" should {
           "update" which {
               "fullname and email" in new UpdateProfileSetup {

                  when( recipientRepositoryMock.updateRecipient( updatedRecipient ))
                        .thenReturn( Future.successful( updatedRecipient ))

                  val result = updateProfile("someuser")

                  status(result) mustBe 303

                  verify( recipientRepositoryMock ).updateRecipient(updatedRecipient)
               }
            }
         }
         "a valid other username" should {
            "return 401" in new UpdateProfileSetup {

               val result = updateProfile("someother")

               status(result) mustBe 401

               verify( recipientLookupMock ).findRecipient("someother")
           }
         }
         "a unknown username" should {
           "return 404" in new UpdateProfileSetup {

               val result = updateProfile("someunknown")

               status(result) mustBe 404


               verify( recipientLookupMock ).findRecipient("someunknown")
           }
        }
      }

      "GET /recipient/:username/password.html" when given {
         "username of current recipient" should {
            "show change password" in new Setup {

               val result = controller.showChangePassword("someuser").apply(
                     FakeRequest().withSession("username"  -> "someuser"))

               status(result) mustBe 200

            }
         }
         "another username than current recipient" should {
            "not show change password" in new Setup {

               val result = controller.showChangePassword("someotheruser").apply(
                     FakeRequest().withSession("username"  -> "someuser"))

               status(result) mustBe 401

            }
         }
      }

      "POST /recipient/:username/password" when given {
         "username of current recipient" when given {
            "valid existing password" which {
               "matching valid new password and confirm" should {
                  "update password" in new Setup {

                     when( recipientRepositoryMock.findCredentials(any[Recipient]))
                        .thenReturn( Future.successful( Some("some-password".bcrypt) ))

                     when( recipientRepositoryMock.updatePassword(any[Recipient]))
                        .thenReturn( Future.successful(recipient) )

                     when( emailNotifierMock.sendPasswordChangedNotification(recipient) )
                           .thenReturn( Future.successful( () ))

                     val result = controller.updatePassword("someuser").apply(
                           FakeRequest().withSession("username"  -> "someuser")
                           .withFormUrlEncodedBody(
                              "password" -> "some-password",
                              "newpassword" -> "some-new-password",
                              "confirm" -> "some-new-password"))

                     status(result) mustBe 303

                  }
               }
               "non matching new password and confirm" should {
                  "not update passwords" in new Setup {

                     val result = controller.updatePassword("someuser").apply(
                           FakeRequest().withSession("username"  -> "someuser")
                           .withFormUrlEncodedBody(
                              "password" -> "some-password",
                              "newpassword" -> "some-new-password",
                              "confirm" -> "some-wrong-password"))

                     status(result) mustBe 400

                     verify( recipientRepositoryMock, never ).findCredentials(any[Recipient])

                  }
               }
            }
            "invalid existing password" should {
               "not update passwords" in new Setup {

                  when( recipientRepositoryMock.findCredentials(any[Recipient]))
                     .thenReturn( Future.successful( Some("some-password".bcrypt) ))

                  val result = controller.updatePassword("someuser").apply(
                        FakeRequest().withSession("username"  -> "someuser")
                        .withFormUrlEncodedBody(
                           "password" -> "some-wrong-password",
                           "newpassword" -> "some-new-password",
                           "confirm" -> "some-new-password"))

                  status(result) mustBe 400

               }
            }
         }
         "another username than ccurent recipient" should {
            "not update passwords" in new Setup {

               val result = controller.updatePassword("someotheruser").apply(
                     FakeRequest().withSession("username"  -> "someuser")
                     .withFormUrlEncodedBody(
                        "password" -> "some-password",
                        "newpassword" -> "some-new-password",
                        "confirm" -> "some-new-password"))

               status(result) mustBe 401

            }
         }
      }
      "GET /password.html" when given {
         "a non logged in visitors" should {
            "show password reset page" in new Setup {

               val result = controller.showResetPassword().apply(FakeRequest())

               status(result) mustBe 200

               val body = ScalaSoup.parse(contentAsString(result))

               body.select("#password-reset-page").headOption mustBe defined
               body.select("#password-reset-page form").headOption mustBe defined
               body.select("#password-reset-page form #inputUsername").headOption mustBe defined
               body.select("#password-reset-page form #inputEmail").headOption mustBe defined
            }
         }
         "a logged in recipient" should {
            "show password reset page for another recipient" in new Setup {
               val result = controller.showResetPassword().apply(
                     FakeRequest().withSession("username"  -> "someuser"))

               status(result) mustBe 200

               val body = ScalaSoup.parse(contentAsString(result))

               body.select("#password-reset-page").headOption mustBe defined

            }
         }
      }
      "POST /password" when given {
         "a registered email address and username and recipient" which is {
            "an anonymous user" should {
               "send reset password email" in new Setup {

                  when( emailNotifierMock.sendPasswordResetEmail(eqTo(recipient), anyString) )
                        .thenReturn( Future.successful( () ))

                  when( recipientRepositoryMock.updatePassword(any[Recipient]))
                     .thenReturn( Future.successful(recipient) )

                  val result = controller.resetPassword().apply(
                        FakeRequest()
                        .withFormUrlEncodedBody(
                           "username" -> "someuser",
                           "email" -> "someuser@example.com"))

                  status(result) mustBe 303

                  verify( recipientLookupMock ).findRecipient("someuser")

                  verify( emailNotifierMock ).sendPasswordResetEmail(eqTo(recipient), anyString)
               }
            }
            "a logged in recipient" should {
               "send reset password email including requestor" in new Setup {

                  when( emailNotifierMock.sendPasswordResetEmail(eqTo(anotherRecipient), anyString) )
                     .thenReturn( Future.successful( () ))

                  when( recipientRepositoryMock.updatePassword(any[Recipient]))
                     .thenReturn( Future.successful(recipient) )

                  val result = controller.resetPassword().apply(
                        FakeRequest()
                        .withSession("username"  -> "someuser")
                        .withFormUrlEncodedBody(
                           "username" -> "someother",
                           "email" -> "someother@example.com"))

                  status(result) mustBe 303

                  verify( recipientLookupMock ).findRecipient("someother")

                  verify( emailNotifierMock ).sendPasswordResetEmail(eqTo(anotherRecipient), anyString)
               }
            }
         }
         "an unknown username" should {
            "not find a recipient to reset" in new Setup {
               val result = controller.resetPassword().apply(
                     FakeRequest()
                     .withFormUrlEncodedBody(
                        "username" -> "someunknown",
                        "email" -> "someunknown@example.com"))

               status(result) mustBe 404

            }
         }
         "an known username but unknown email" should {
            "not find a recipient to reset" in new Setup {
               val result = controller.resetPassword().apply(
                     FakeRequest()
                     .withFormUrlEncodedBody(
                        "username" -> "someuser",
                        "email" -> "someunknown@example.com"))

               status(result) mustBe 404

            }
         }

      }
      "POST /recipient/verify" should {
         "send email notification" when given {
            "valid username, email, password and recipient is unverified" in new Setup {

               when(recipientRepositoryMock.findCredentials(recipient))
                  .thenReturn( Future.successful( Some( "some-password".bcrypt ) ) )
               when(featureTogglesMock.isEnabled( FeatureToggle.EmailVerification ))
                  .thenReturn(true)
               when(recipientRepositoryMock.isEmailVerified(recipient))
                  .thenReturn( Future.successful(false))
               when(recipientRepositoryMock.findVerificationHash(recipient))
                  .thenReturn( Future.successful( Some("some-verification-hash")))
               when( emailNotifierMock.sendEmailVerification(recipient, "some-verification-hash"))
                  .thenReturn( Future.successful(()))

               val result = controller.resendVerification().apply(
                     FakeRequest()
                     .withFormUrlEncodedBody(
                        "username" -> "someuser",
                        "email"    -> "someuser@example.com",
                        "password" -> "some-password"))

               status(result) mustBe 303

               verify( emailNotifierMock )
                  .sendEmailVerification(recipient, "some-verification-hash")
            }
         }
         "not send email notification" when given {
            "unknown username" in new Setup {

               val result = controller.resendVerification().apply(
                     FakeRequest()
                     .withFormUrlEncodedBody(
                        "username" -> "someunknown",
                        "email"    -> "someunknown@example.com",
                        "password" -> "some-password"))

               status(result) mustBe 404

               verifyZeroInteractions( emailNotifierMock )
            }
            "invalid email" in new Setup {

               val result = controller.resendVerification().apply(
                     FakeRequest()
                     .withFormUrlEncodedBody(
                        "username" -> "someuser",
                        "email"    -> "someunknown@example.com",
                        "password" -> "some-password"))

               status(result) mustBe 404

               verifyZeroInteractions( emailNotifierMock )
            }
            "invalid password"  in new Setup {

               when(recipientRepositoryMock.findCredentials(recipient))
                     .thenReturn( Future.successful( Some( "some-password".bcrypt ) ) )

               val result = controller.resendVerification().apply(
                     FakeRequest()
                     .withFormUrlEncodedBody(
                        "username" -> "someuser",
                        "email"    -> "someuser@example.com",
                        "password" -> "some-other-password"))

               status(result) mustBe 401

               verify(recipientRepositoryMock).findCredentials(recipient)
               verifyZeroInteractions( emailNotifierMock )
            }

            "verification is not enabled" in new Setup {

               when(recipientRepositoryMock.findCredentials(recipient))
                  .thenReturn( Future.successful( Some( "some-password".bcrypt ) ) )
               when(featureTogglesMock.isEnabled( FeatureToggle.EmailVerification ))
                  .thenReturn(false)

               val result = controller.resendVerification().apply(
                     FakeRequest()
                     .withFormUrlEncodedBody(
                        "username" -> "someuser",
                        "email"    -> "someuser@example.com",
                        "password" -> "some-password"))

               status(result) mustBe 303
               verifyZeroInteractions( emailNotifierMock )
            }

            "email is already verified" in new Setup {

               when(recipientRepositoryMock.findCredentials(recipient))
                  .thenReturn( Future.successful( Some( "some-password".bcrypt ) ) )
               when(featureTogglesMock.isEnabled( FeatureToggle.EmailVerification ))
                  .thenReturn(true)
               when(recipientRepositoryMock.isEmailVerified(recipient))
                  .thenReturn( Future.successful(true))

               val result = controller.resendVerification().apply(
                     FakeRequest()
                     .withFormUrlEncodedBody(
                        "username" -> "someuser",
                        "email"    -> "someuser@example.com",
                        "password" -> "some-password"))

               status(result) mustBe 303
               verifyZeroInteractions( emailNotifierMock )
            }
         }
      }
   }
}
