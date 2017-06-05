package models

import com.google.inject.ImplementedBy
import javax.inject.Singleton

@ImplementedBy(classOf[DefaultRecipientFactory])
trait RecipientFactory {

   def newRecipient(registerForm: (String, Option[String], String, String, String)): Recipient =
      Recipient(
         recipientId = None,
         username    = registerForm._1,
         fullname    = registerForm._2,
         email       = registerForm._3,
         password    = Some(registerForm._4),
         isAdmin     = false )
}

@Singleton
class DefaultRecipientFactory extends RecipientFactory
