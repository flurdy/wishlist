package models

import com.google.inject.ImplementedBy
import javax.inject.Singleton
import scala.concurrent.Future

@ImplementedBy(classOf[DefaultRecipientLookup])
trait RecipientLookup {

   def findRecipient(username: String): Future[Option[Recipient]] = ???

}

@Singleton
class DefaultRecipientLookup extends RecipientLookup
