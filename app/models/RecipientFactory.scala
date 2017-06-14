package models

import com.github.t3hnar.bcrypt._
import com.google.inject.ImplementedBy
import javax.inject.Singleton

@ImplementedBy(classOf[DefaultRecipientFactory])
trait RecipientFactory {

   def newRecipient(registerForm: (String, Option[String], String, String, String)): Recipient =
      Recipient(
         recipientId = None,
         username    = registerForm._1.trim.toLowerCase,
         fullname    = registerForm._2.map(_.trim),
         email       = registerForm._3.trim,
         password    = Some(registerForm._4.trim.bcrypt),
         isAdmin     = false )
}

@Singleton
class DefaultRecipientFactory extends RecipientFactory
