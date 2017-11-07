package controllers

import akka.stream.Materializer
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any,anyString,eq => eqTo}
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.{ExecutionContext,Future}
import com.flurdy.wishlist.ScalaSoup
import models._
import notifiers._
import repositories._


class RegisterControllerSpec extends BaseUnitSpec with Results with GuiceOneAppPerSuite with TableDrivenPropertyChecks {

   val invalidEmailAddresses =
      Table(
         ("invalid email address", "error message"),
         ("@example.com", "no alias"),
         ("john.example.com", "no @"),
         ("example@-example.com", "invalid start of domain"),
         ("example@", "no domain or tld"),
         ("&*@c.com", "invalid characters"), // technically valid but too fragile
         ("example@c", "too short domain"),
         ("example@example@example.com", "too many @"),
         ("john+smith+jones@example.com", "too many +"),
         ("john..smith@example.com", "no double dotting")
      )

   val validEmailAddresses =
      Table(
         ("valid email address", "description"),
         ("john@example.com","standard alias@domain.tld"),
         ("john@no","no domain just tld"),
         ("john@example.company","new tld"),
         ("john.smith-jones.olsen@example.com","a long complicated name"),
         ("john.Smith@Example.com","ignoring caps"),
         ("john.smith+alibaba@example.com","Allowing + aliases"),
         ("john-smith_jones@example.com","Allowing dash and underscore"),
         ("john@10.10.10.10","Allowing ip addresses"),
         ("john@123.com","Allowing numeric domains")
      )

   val validUsernames =
      Table(
         ("valid username", "description"),
         ("abc", "3 chars or more"),
         ("abc-abc", "with dashes"),
         ("abc_abc", "with underscore"),
         ("_abc", "starting with underscore"),
         (" abc ", "trimming leading or trailing spaces")
      )

   val invalidUsernames =
      Table(
         ("invalid username", "error message"),
         ("co", "too short"),
         ("&*@c.com", "invalid characters")
      )

   trait Setup {
      val configurationMock = mock[Configuration]
      val appConfigMock = mock[ApplicationConfig]
      val recipientFactoryMock = mock[RecipientFactory]
      val recipientLookupMock = mock[RecipientLookup]
      val recipientRepositoryMock = mock[RecipientRepository]
      val featureTogglesMock = mock[FeatureToggles]
      val emailNotifierMock = mock[EmailNotifier]
      val usernameValidatorMock = mock[UsernameValidator]
      val controller = new RegisterController(configurationMock, recipientFactoryMock, recipientLookupMock, emailNotifierMock, appConfigMock, usernameValidatorMock)(recipientRepositoryMock, featureTogglesMock)
      when(appConfigMock.getString(anyString)).thenReturn(None)
   }

   trait RegisterSetup extends Setup {
      val recipientMock = mock[Recipient]
      val registerForm = ("some-username", Some("some name"), "some-email@example.com", "some-password", "some-password")
      val registerRequest = FakeRequest().withFormUrlEncodedBody(
         "fullname" -> "some name",
         "username" -> "some-username",
         "email"    -> "some-email@example.com",
         "password" -> "some-password",
         "confirm"  -> "some-password")
      when ( usernameValidatorMock.isValid( anyString )(any[ExecutionContext]) ).thenReturn( Future.successful( true ) )
   }

   "Register controller" when requesting {

      "[GET] /register.html" should {
         "prefill the register form" in new Setup {
            val request = FakeRequest().withFormUrlEncodedBody("email" -> "some-username")

            val result = controller.redirectToRegisterForm().apply(request)

            status(result) mustBe 200

            val bodyDom = ScalaSoup.parse(contentAsString(result))
            bodyDom.select(s"#register-page").headOption mustBe defined
            bodyDom.select(s"form #inputUsername").headOption.value.attr("value") mustBe "some-username"
            bodyDom.select(s"form #inputEmail").headOption.value.attr("value") mustBe "some-username"
         }
      }

      "[GET] /register/" should {
         "show register form" in new Setup {

            val result = controller.redirectToRegisterForm().apply(FakeRequest())

            status(result) mustBe 200

            val bodyDom = ScalaSoup.parse(contentAsString(result))
            bodyDom.select(s"#register-page").headOption mustBe defined
            bodyDom.select(s"form #inputUsername").headOption.value.attr("value") mustBe ""
         }

         "not show register form given a logged in session" in new Setup {
            pending
         }
      }

      "[POST] /register/" must {
         "register" which {
            "creates a recipient" in new RegisterSetup {

               when ( recipientLookupMock.findRecipient( "some-username" ) )
                  .thenReturn( Future.successful( None ) )
               when ( recipientFactoryMock.newRecipient( registerForm ) ).thenReturn( recipientMock )
               when ( recipientMock.save()(recipientRepositoryMock) ).thenReturn( Future.successful( recipientMock ) )

               val result = controller.register().apply(registerRequest)

               status(result) mustBe 303
               header("Location", result).value mustBe "/"

               verify ( recipientMock ).save()(recipientRepositoryMock)
            }

            "sends a verification email" in new RegisterSetup {

               when ( recipientLookupMock.findRecipient( "some-username" ) )
                  .thenReturn( Future.successful( None ) )
               when ( recipientFactoryMock.newRecipient( registerForm ) ).thenReturn( recipientMock )
               when ( recipientMock.save()(recipientRepositoryMock) )
                  .thenReturn( Future.successful( recipientMock ) )
               when ( recipientMock.findOrGenerateVerificationHash(recipientRepositoryMock) )
                  .thenReturn( Future.successful( "some-verification-hash" ) )
               when ( recipientMock.username )
                     .thenReturn( "some-username" )
               when( featureTogglesMock.isEnabled(FeatureToggle.EmailVerification) )
                  .thenReturn( true )
               when( emailNotifierMock.sendEmailVerification(eqTo(recipientMock), anyString )) // "/recipient/some-username/verify/some-verification-hash/"))
                  .thenReturn( Future.successful(()))

               val result = controller.register().apply(registerRequest)

               status(result) mustBe 303

               verify ( recipientMock ).save()(recipientRepositoryMock)
               verify( featureTogglesMock ).isEnabled(FeatureToggle.EmailVerification)
               verify( emailNotifierMock ).sendEmailVerification(recipientMock, "/recipient/some-username/verify/some-verification-hash/")

            }

            "sends new registration alert" in new RegisterSetup {

               when ( recipientLookupMock.findRecipient( "some-username" ) )
                  .thenReturn( Future.successful( None ) )
               when ( recipientFactoryMock.newRecipient( registerForm ) ).thenReturn( recipientMock )
               when ( recipientMock.save()(recipientRepositoryMock) ).thenReturn( Future.successful( recipientMock ) )

               val result = controller.register().apply(registerRequest)

               status(result) mustBe 303

               verify ( recipientMock ).save()(recipientRepositoryMock)
               verify( emailNotifierMock ).sendNewRegistrationAlert(recipientMock)
            }


            forAll(validEmailAddresses) { (emailAddress, description) =>
               s"valid email addresses: $description" in new RegisterSetup {

                  val validRegisterForm = ("some-username", Some("some name"), emailAddress, "some-password", "some-password")

                  when ( recipientLookupMock.findRecipient( "some-username" ) )
                        .thenReturn( Future.successful( None ) )
                  when ( recipientFactoryMock.newRecipient( validRegisterForm ) ).thenReturn( recipientMock )
                  when ( recipientMock.save()(recipientRepositoryMock) ).thenReturn( Future.successful( recipientMock ) )

                  val validRegisterRequest = FakeRequest().withFormUrlEncodedBody(
                     "fullname" -> "some name",
                     "username" -> "some-username",
                     "email"    -> emailAddress,
                     "password" -> "some-password",
                     "confirm"  -> "some-password")

                  val result = controller.register().apply(validRegisterRequest)

                  status(result) mustBe 303

                  verify ( recipientMock ).save()(recipientRepositoryMock)

               }
            }

            forAll(validUsernames) { (username, description) =>
               s"valid username: $description" in new RegisterSetup {

                  val validRegisterForm = (username, Some("some name"), "some@example.com", "some-password", "some-password")


                  when ( recipientLookupMock.findRecipient( username.trim ) )
                        .thenReturn( Future.successful( None ) )
                  when ( recipientFactoryMock.newRecipient( validRegisterForm ) ).thenReturn( recipientMock )
                  when ( recipientMock.save()(recipientRepositoryMock) ).thenReturn( Future.successful( recipientMock ) )

                  val validRegisterRequest = FakeRequest().withFormUrlEncodedBody(
                     "fullname" -> "some name",
                     "username" -> username,
                     "email"    -> "some@example.com",
                     "password" -> "some-password",
                     "confirm"  -> "some-password")

                  val result = controller.register().apply(validRegisterRequest)

                  status(result) mustBe 303

                  verify ( recipientMock ).save()(recipientRepositoryMock)

               }
            }

         }
         "not register" when given {
            "an already registered username" in new RegisterSetup {

               when ( recipientLookupMock.findRecipient( "some-username" ) )
                     .thenReturn( Future.successful( Some(recipientMock) ) )

               val result = controller.register().apply(registerRequest)

               status(result) mustBe 400

               verifyZeroInteractions( recipientFactoryMock, recipientMock )
            }

            "an invalid username that is too short" in new RegisterSetup {

               val invalidRegisterRequest = FakeRequest().withFormUrlEncodedBody(
                  "fullname" -> "some name",
                  "username" -> "so",
                  "email"    -> "some-email@example.com",
                  "password" -> "some-password",
                  "confirm"  -> "some-password")

               val result = controller.register().apply(invalidRegisterRequest)

               status(result) mustBe 400

               verifyZeroInteractions( recipientFactoryMock, recipientMock )
             }

            "an invalid username with unexpected characters" in new RegisterSetup {

               val invalidRegisterRequest = FakeRequest().withFormUrlEncodedBody(
                  "fullname" -> "some name",
                  "username" -> "so+meusername%",
                  "email"    -> "some-email@example.com",
                  "password" -> "some-password",
                  "confirm"  -> "some-password")

               val result = controller.register().apply(invalidRegisterRequest)

               status(result) mustBe 400

               verifyZeroInteractions( recipientFactoryMock, recipientMock )
             }

            "an bad username, ie unwanted username" in new RegisterSetup {

               when ( recipientLookupMock.findRecipient( "admin" ) )
                     .thenReturn( Future.successful( None ) )
               when ( usernameValidatorMock.isValid( eqTo("admin") )(any[ExecutionContext]) ).thenReturn( Future.successful( false ) )

               val invalidRegisterRequest = FakeRequest().withFormUrlEncodedBody(
                  "fullname" -> "some name",
                  "username" -> "admin",
                  "email"    -> "some-email@example.com",
                  "password" -> "some-password",
                  "confirm"  -> "some-password")

               val result = controller.register().apply(invalidRegisterRequest)

               status(result) mustBe 400

               verifyZeroInteractions( recipientFactoryMock, recipientMock )
             }

            "an invalid password that is too short" in new RegisterSetup {

               val invalidRegisterRequest = FakeRequest().withFormUrlEncodedBody(
                  "fullname" -> "some name",
                  "username" -> "some-username",
                  "email"    -> "some-email@example.com",
                  "password" -> "so",
                  "confirm"  -> "so")

               val result = controller.register().apply(invalidRegisterRequest)

               status(result) mustBe 400

               verifyZeroInteractions( recipientFactoryMock, recipientMock )
            }

            "a password that does not match confirm password" in new RegisterSetup {

               val invalidRegisterRequest = FakeRequest().withFormUrlEncodedBody(
                  "fullname" -> "some name",
                  "username" -> "some-username",
                  "email"    -> "some-email@example.com",
                  "password" -> "some-other-password",
                  "confirm"  -> "some-password")

               val result = controller.register().apply(invalidRegisterRequest)

               status(result) mustBe 400

               verifyZeroInteractions( recipientFactoryMock, recipientMock )
            }

            forAll(invalidEmailAddresses) { (emailAddress, error) =>
               s"invalid email addresses: $error" in new RegisterSetup {

                  val invalidRegisterRequest = FakeRequest().withFormUrlEncodedBody(
                     "fullname" -> "some name",
                     "username" -> "some-username",
                     "email"    -> emailAddress,
                     "password" -> "some-password",
                     "confirm"  -> "some-password")

                  val result = controller.register().apply(invalidRegisterRequest)

                  status(result) mustBe 400

                  verifyZeroInteractions( recipientFactoryMock, recipientMock )
               }
            }

            forAll(invalidUsernames) { (username, error) =>
               s"invalid username: $error" in new RegisterSetup {

                  val invalidRegisterRequest = FakeRequest().withFormUrlEncodedBody(
                     "fullname" -> "some name",
                     "username" -> username,
                     "email"    -> "some@example.com",
                     "password" -> "some-password",
                     "confirm"  -> "some-password")

                  val result = controller.register().apply(invalidRegisterRequest)

                  status(result) mustBe 400

                  verifyZeroInteractions( recipientFactoryMock, recipientMock )
               }
            }

         }
      }
   }

}
