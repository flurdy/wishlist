package models

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Environment
import scala.concurrent.ExecutionContext.Implicits.global
import controllers.BaseUnitSpec


class UsernameValidatorSpec extends BaseUnitSpec with IntegrationPatience with ScalaFutures with TableDrivenPropertyChecks {

   trait Setup {
      val environment = Environment.simple()
      val fileLoader = new DefaultFileLoader(environment)
      val usernameValidator = new DefaultUsernameValidator(fileLoader)
   }

   val usernames =
      Table(
         ("username","isValid"),
         ("johnsmith", true),
         ("supportguy", true),
         ("admin___", true),
         ("admin", false),
         ("support", false),
         ("all", false),
         ("mail", false)
      )

   "Username Validator" when given {
      forAll(usernames) { (username, isValid) =>
         s"be $isValid for username $username" in new Setup {
            whenReady( usernameValidator.isValid(username)){ response =>
               response mustBe isValid
            }
         }
      }
   }
}
