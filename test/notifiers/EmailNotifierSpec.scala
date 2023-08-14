// package notifiers

// import org.mockito.Mockito._
// import org.mockito.ArgumentMatchers.anyString
// import org.scalatest._
// import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
// import org.scalatest.mockito.MockitoSugar
// import org.scalatestplus.play._
// import play.api.test._
// import play.api.test.Helpers._
// import scala.concurrent.Future
// import controllers.BaseUnitSpec
// import models.Recipient

// class EmailNotifierSpec extends BaseUnitSpec with ScalaFutures {

//    trait Setup {
//       val dispatcherMock = mock[EmailDispatcher]
//       val templatesMock  = mock[EmailTemplates]
//       val emailNotifier  = new DefaultEmailNotifier(dispatcherMock, templatesMock)

//       val message = EmailMessage("message subject", "message body")
//       val recipient = new Recipient("someuser").copy(email = "someone@example.com")
//    }

//    trait ContactSetup extends Setup {
//       when( dispatcherMock.sendContactEmail(message) ).thenReturn( Future.successful(()) )
//    }

//    trait NotificationSetup extends Setup {
//       when( dispatcherMock.sendNotificationEmail( "someone@example.com", message) ).thenReturn( Future.successful(()) )
//    }

//    trait AlertSetup extends Setup {
//       when( dispatcherMock.sendAlertEmail(message) ).thenReturn( Future.successful(()) )
//    }

//    "sendContactEmail" should {
//       "send message" when given {
//          "mandatory fields"  in new ContactSetup {
//             when( templatesMock.contactMessageText(
//                   "some-name", "some@example.com",
//                   username = None, subject = None,
//                   "some message", currentRecipient = None)
//                ).thenReturn( message )

//             whenReady ( emailNotifier.sendContactEmail(
//                   "some-name", "some@example.com",
//                   username = None, subject = None,
//                   "some message", currentRecipient = None) ){ response =>

//                verify(dispatcherMock).sendContactEmail(message)
//             }
//          }

//          "optional fields" in new ContactSetup {
//             when( templatesMock.contactMessageText(
//                   "some-name", "some@example.com",
//                   Some("someuser"), Some("Some subject"),
//                   "some message", Some(recipient))
//                ).thenReturn( message )

//             whenReady ( emailNotifier.sendContactEmail(
//                   "some-name", "some@example.com",
//                   Some("someuser"), Some("Some subject"),
//                   "some message", Some(recipient)) ){ response =>

//                verify(dispatcherMock).sendContactEmail(message)
//             }
//          }
//       }
//    }

//    "sendNewRegistrationAlert" should {
//       "send message" in new AlertSetup {

//          when( templatesMock.registrationAlertText( "someuser" ) ).thenReturn( message )

//          whenReady ( emailNotifier.sendNewRegistrationAlert(recipient) ){ response =>

//             verify(dispatcherMock).sendAlertEmail(message)
//          }
//       }
//    }

//    "sendRecipientDeletedAlert" should {
//       "send message" in new AlertSetup {

//          when( templatesMock.deleteRecipientAlertText( "someuser" ) ).thenReturn( message )

//          whenReady ( emailNotifier.sendRecipientDeletedAlert(recipient) ){ response =>

//             verify(dispatcherMock).sendAlertEmail(message)
//          }
//       }
//    }

//    "sendRecipientDeletedNotification" should {
//       "send message" in new NotificationSetup {

//          when( templatesMock.deleteRecipientNotificationText( "someuser" ) ).thenReturn( message )

//          whenReady ( emailNotifier.sendRecipientDeletedNotification(recipient) ){ response =>

//             verify(dispatcherMock).sendNotificationEmail("someone@example.com", message)
//          }
//       }
//    }

//    "sendEmailVerification" should {
//       "send message" in new NotificationSetup {

//          when( templatesMock.emailVerificationText( "someuser", "some-hash" ) ).thenReturn( message )

//          whenReady ( emailNotifier.sendEmailVerification(recipient, "some-hash") ){ response =>

//             verify(dispatcherMock).sendNotificationEmail("someone@example.com", message)
//          }
//       }
//    }

//    "sendPasswordResetEmail" should {
//       "send message" in new NotificationSetup {

//          when( templatesMock.newPasswordText( anyString ) ).thenReturn( message )

//          whenReady ( emailNotifier.sendPasswordResetEmail(recipient, "some-password") ){ response =>

//             verify(dispatcherMock).sendNotificationEmail("someone@example.com", message)
//          }
//       }
//    }

//    "sendPasswordChangedNotification" should {
//       "send message" in new NotificationSetup {

//          when( templatesMock.changePasswordText ).thenReturn( message )

//          whenReady ( emailNotifier.sendPasswordChangedNotification(recipient) ){ response =>

//             verify(dispatcherMock).sendNotificationEmail("someone@example.com", message)
//          }
//       }
//    }
// }
