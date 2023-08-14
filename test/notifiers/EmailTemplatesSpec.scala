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
// import models.{EmailConfig,Recipient}

// class EmailTemplatesSpec extends BaseUnitSpec with ScalaFutures {

//    trait Setup {
//       val emailConfig = mock[EmailConfig]
//       when(emailConfig.hostname).thenReturn("wish.example.com")
//       val templates = new DefaultEmailTemplates(emailConfig)
//    }

//    "footerText" should respondWith {
//       "hostname" in new Setup {
//          val message = templates.footer
//          message must include ("Visit: http://wish.example.com")
//       }
//    }

//    "registrationAlertText" should respondWith {
//       "username" in new Setup {
//          val message = templates.registrationAlertText("someuser")
//          message.body must include ("someuser")
//       }
//    }

//    "deleteRecipientAlertText" should respondWith {
//       "username" in new Setup {
//          val message = templates.deleteRecipientAlertText("someuser")
//          message.body must include ("someuser")
//       }
//    }

//    "deleteRecipientNotificationText" should respondWith {
//       "username" in new Setup {
//          val message = templates.deleteRecipientNotificationText("someuser")
//          message.body must include ("someuser")
//       }
//    }

//    "newPasswordText" should respondWith {
//       "username" in new Setup {
//          val message = templates.newPasswordText("some-password")
//          message.body must include ("some-password")
//       }
//    }

//    "emailVerificationText" should respondWith {
//       "username" in new Setup {
//          val message = templates.emailVerificationText(
//             "someuser","/recipient/someuser/verify/some-verification/ ")
//          message.body must include ("/recipient/someuser/verify/some-verification/")
//          message.body must include ("http://wish.example.com/recipient/someuser/verify/some-verification/")
//       }
//    }

//    "contactMessageText" should respondWith {
//       "username" in new Setup {
//          val message = templates.contactMessageText(
//                "some-name", "someone@example.com",
//                Some("someotheruser"),
//                Some("some subject"), "some message",
//                currentRecipient = Some(new Recipient("someuser"))
//             )
//          message.body must include ("some-name")
//          message.body must include ("someotheruser")
//          message.body must include ("some subject")
//          message.body must include ("some message")
//          message.body must include ("someuser")
//       }
//       "not recipient" in new Setup {
//          val message = templates.contactMessageText(
//                "some-name", "someone@example.com",
//                Some("someotheruser"),
//                Some("some subject"), "some message",
//                currentRecipient = None
//             )
//          message.body must include ("some-name")
//          message.body must include ("someotheruser")
//          message.body must include ("some subject")
//          message.body must include ("some message")
//          message.body must not include ("someuser")
//       }
//    }
// }
