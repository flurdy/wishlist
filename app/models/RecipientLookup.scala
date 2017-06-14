package models

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import repositories._

@ImplementedBy(classOf[DefaultRecipientLookup])
trait RecipientLookup {

   def recipientRepository: RecipientRepository

   def findRecipient(username: String) = recipientRepository.findRecipient(username)

}

@Singleton
class DefaultRecipientLookup @Inject() (val recipientRepository: RecipientRepository) extends RecipientLookup
