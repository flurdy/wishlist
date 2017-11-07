
package models

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.Environment
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[DefaultUsernameValidator])
trait UsernameValidator {

   def fileLoader: FileLoader

   def isValid(username: String)(implicit ec: ExecutionContext): Future[Boolean] = isInvalid(username).map(!_)

   protected def isInvalid(username: String)(implicit ec: ExecutionContext): Future[Boolean] =
      Future {
         fileLoader.resourceAsStream("bad_usernames.en.json")
            .map ( j => (Json.parse(j) \ "usernames").as[List[String]] ) match {
               case Some(usernames) =>
                  usernames.exists(_.trim.toLowerCase == username.trim.toLowerCase)
               case _ => throw new IllegalStateException("Unable to read bad usernames")
            }
      }
}

@Singleton
class DefaultUsernameValidator @Inject() (val fileLoader: FileLoader) extends UsernameValidator

@ImplementedBy(classOf[DefaultFileLoader])
trait FileLoader {

   def environment: Environment

   def resourceAsStream = environment.resourceAsStream _

}

@Singleton
class DefaultFileLoader @Inject() (val environment: Environment) extends FileLoader
