package models

import com.github.t3hnar.bcrypt._
import play.Logger
import play.api.libs.concurrent.Execution.Implicits._
import java.math.BigInteger
import java.security.SecureRandom
import scala.concurrent.Future
import repositories._


case class Recipient (
      recipientId: Option[Long],
      username: String,
      fullname: Option[String],
      email: String,
      password: Option[String],
      isAdmin: Boolean=false
){

   def this(recipientId: Long) = this(Some(recipientId), "", None, "", None, false)

   def this(username: String) = this(None, username, None, "", None, false)

   def save()(implicit recipientRepository: RecipientRepository) =
      recipientRepository.saveRecipient(this)

   def authenticate(possiblePassword: String)(implicit recipientRepository: RecipientRepository) =
      recipientRepository.findCredentials(this).map {
         case Some(passwordFound) => possiblePassword.isBcrypted(passwordFound)
         case _ => false
      }

   def isVerified(implicit recipientRepository: RecipientRepository) =
      recipientRepository.isEmailVerified(this)

   def findWishlists(implicit wishlistRepository: WishlistRepository): Future[List[Wishlist]] =
      wishlistRepository.findRecipientWishlists(this)

   def findOrGenerateVerificationHash(implicit recipientRepository: RecipientRepository): Future[String] = findVerificationHash.flatMap {
      case Some(verificationHash) => Future.successful( verificationHash )
      case _ => generateVerificationHash
   }

   private def findVerificationHash(implicit recipientRepository: RecipientRepository): Future[Option[String]] = recipientRepository.findVerificationHash(this)

   private def generateVerificationHash(implicit recipientRepository: RecipientRepository): Future[String] = {
      val verificationHash = new BigInteger(130,  new SecureRandom()).toString(32)
      recipientRepository.saveVerificationHash(this, verificationHash)
   }



  /*

  def this(recipientId:Long, username:String) = this(Some(recipientId),username,None,"",None,false)

  def delete = Recipient.delete(this)

  def update = Recipient.update(this)

  def resetPassword = Recipient.resetPassword(this)

  def updatePassword(newPassword:String) {
    Recipient.updatePassword(this.copy(password=Some(newPassword)))
  }

  def findWishlists = Wishlist.findByRecipient(this)

  def findOrganisedWishlists = Wishlist.findByOrganiser(this)

  def findReservations : Seq[Reservation] = Reservation.findByRecipient(this)

  def doesVerificationMatch(verificationHash:String) = Recipient.doesVerificationMatch(this,verificationHash)

  def isEmailVerified = Recipient.isEmailVerified(this)

  def setEmailAsVerified = Recipient.setEmailAsVerified(this)
*/

  def isSame(other: Recipient) = recipientId.fold(false)( _ => recipientId == other.recipientId )

  def canEdit(wishlist:Wishlist)(implicit wishlistLookup: WishlistLookup) =
      wishlist.isOrganiser(this).map( _ || isAdmin || wishlist.recipient.isSame(this) )

/*
  def findEditableWishlists = Wishlist.findEditableWishlists(this)
  */
}



object Recipient {
 /*

  val emailVerificationRequired = Play.configuration.getString("mail.verification").getOrElse("false") == "true"

  val simple = {
    get[Long]("recipientid") ~
    get[String]("username") ~
    get[Option[String]]("fullname") ~
    get[String]("email") ~
    get[Boolean]("isAdmin")  map {
    case recipientid~username~fullname~email~isadmin => Recipient( Some(recipientid), username, fullname, email, None, isadmin)
    }
  }

  val authenticationMapper = {
    get[Long]("recipientid") ~
      get[String]("username") ~
      get[Option[String]]("password")  map {
      case recipientid~username~password => Recipient( Some(recipientid), username, null, null, password )
    }
  }

  def encrypt(passwordOption: Option[String]) = {
    passwordOption.map { password =>
      Some(BCrypt.hashpw(password,BCrypt.gensalt()))
    }.getOrElse(None)
  }


  def save(recipient:Recipient) = {
      Logger.debug("Inserting recipient: "+recipient.username)
      findByUsername(recipient.username) match {
          case None => {
              DB.withConnection { implicit connection =>
                  val nextRecipientId = SQL("SELECT NEXTVAL('recipient_seq')").as(scalar[Long].single)
                  SQL(
                      """
                          insert into recipient
                          (recipientid,username,fullname,email,password,isadmin)
                          values
                          ({recipientid},{username},{fullname},{email},{password},false)
                      """
                  ).on(
                      'recipientid -> nextRecipientId,
                      'username -> recipient.username,
                      'fullname -> recipient.fullname,
                      'email -> recipient.email,
                      'password -> encrypt(recipient.password)
                  ).executeInsert()
                  recipient.copy(recipientId = Some(nextRecipientId),password=None)
              }
          }
          case Some(existingRecipient) => throw new IllegalArgumentException("Username already exists")
      }
  }

    def findByUsername(username:String) : Option[Recipient]= {
        DB.withConnection { implicit connection =>
          SQL(
            """
              SELECT * FROM recipient
                WHERE username = {username}
            """
          ).on(
            'username -> username.trim
          ).as(Recipient.simple.singleOpt)
        }
    }

    def findByUsernameAndEmail(username:String,email:String) : Option[Recipient]= {
        DB.withConnection { implicit connection =>
          SQL(
            """
              SELECT * FROM recipient
                WHERE username = {username}
                AND email = {email}
            """
          ).on(
            'username -> username.trim,
            'email -> email.trim
          ).as(Recipient.simple.singleOpt)
        }
    }

    def findById(recipientId:Long) : Option[Recipient]= {
        DB.withConnection { implicit connection =>
          SQL(
            """
              SELECT * FROM recipient
                WHERE recipientid = {recipientid}
            """
          ).on(
            'recipientid -> recipientId
          ).as(Recipient.simple.singleOpt)
        }
    }


    def findAuthenticationDetailsByUsername(username:String) : Option[Recipient]= {
        DB.withConnection { implicit connection =>
          SQL(
            """
              SELECT * FROM recipient
                WHERE username = {username}
            """
          ).on(
            'username -> username.trim
          ).as(Recipient.authenticationMapper.singleOpt)
        }
    }



  def delete(recipient:Recipient) {
    Logger.debug("Deleting recipient: "+ recipient.username)
    Wishlist.findByRecipient(recipient).map { wishlist =>
      wishlist.delete
    }
    DB.withConnection { implicit connection =>
      SQL(
        """
                    delete from recipient
                    where recipientid = {recipientid}
        """
      ).on(
        'recipientid -> recipient.recipientId
      ).execute()
    }
  }



  def update(recipient:Recipient) {
    Logger.debug("Updating recipient: "+recipient.username)
    DB.withConnection { implicit connection =>
      SQL(
        """
            update recipient
            set fullname = {fullname},
            email = {email}
            where recipientid = {recipientid}
        """
      ).on(
        'recipientid -> recipient.recipientId,
        'fullname -> recipient.fullname,
        'email -> recipient.email
      ).executeUpdate()
    }
  }



  def updatePassword(recipient:Recipient) {
    Logger.debug("Updating password for recipient: "+recipient.username)
    DB.withConnection { implicit connection =>
      SQL(
        """
            update recipient
            set password = {password}
            where recipientid = {recipientid}
        """
      ).on(
        'recipientid -> recipient.recipientId,
        'password -> encrypt(recipient.password)
      ).executeUpdate()
    }
  }



  def generateRandomPassword = {
    val source = new BigInteger(130,  new SecureRandom()).toString(32)
    source.substring(0,3)+"-"+source.substring(4,7)+"-"+source.substring(8,11)+"-"+source.substring(12,15)
  }

  def resetPassword(recipient:Recipient) : String = {
      val newPassword = generateRandomPassword
      updatePassword( recipient.copy(password=Option(newPassword)) )
      newPassword
  }



  def isEmailVerifiedOrNotRequired(username: String, password: String) : Option[Recipient]= {
    authenticate(username,password).map { authenticatedRecipient =>
      if( !emailVerificationRequired || authenticatedRecipient.isEmailVerified ){
        return Some(authenticatedRecipient)
      }
    }
    return None
  }



  def generateVerificationHash = new BigInteger(130,  new SecureRandom()).toString(32)


  def saveVerification(recipient:Recipient, verificationHash: String) {
    DB.withConnection { implicit connection =>
      SQL(
        """
              INSERT INTO emailverification
              (recipientid,email,verificationhash)
              VALUES
              ({recipientid},{email},{verificationhash})
        """
      ).on(
        'recipientid -> recipient.recipientId ,
        'email -> recipient.email,
        'verificationhash -> verificationHash
      ).executeInsert()
    }
  }

  def doesVerificationMatch(recipient:Recipient, verificationHash: String) : Boolean = {
    DB.withConnection { implicit connection =>
      SQL(
        """
              SELECT count(*) = 1 FROM emailverification
              WHERE recipientid = {recipientid}
              AND email = {email}
              AND verificationhash = {verificationhash}
        """
      ).on(
        'recipientid -> recipient.recipientId ,
        'email -> recipient.email,
        'verificationhash -> verificationHash
      ).as(scalar[Boolean].single)
    }
  }


  def setEmailAsVerified(recipient:Recipient) {
    DB.withConnection { implicit connection =>
      SQL(
        """
              UPDATE emailverification
              set verified = true
              WHERE recipientid = {recipientid}
        """
      ).on(
        'recipientid -> recipient.recipientId
      ).execute()
    }
  }

  def findVerificationHash(recipient:Recipient) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
              SELECT verificationhash FROM emailverification
              WHERE recipientid = {recipientid}
        """
      ).on(
        'recipientid -> recipient.recipientId
      ).as(scalar[String].singleOpt)
    }
  }
 */
}
