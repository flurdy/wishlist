package models


// import play.api.Play.current
// import org.mindrot.jbcrypt.BCrypt
// import play.api.db.DB
// import anorm._
// import anorm.SqlParser._
import play.Logger
// import java.math.BigInteger
// import java.security.{SecureRandom, MessageDigest}
// import play.api.Play
import scala.concurrent.Future


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

  def save(): Future[Either[_,Recipient]] = Future.successful( Right(this) ) // Recipient.save(this)

  def authenticate(possiblePassword: String): Future[Boolean] = ???

  def isVerified: Future[Boolean] = ???

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

  def generateVerificationHash = {
    val verificationHash = Recipient.generateVerificationHash
    Recipient.saveVerification(this,verificationHash)
    verificationHash
  }

  def findVerificationHash = Recipient.findVerificationHash(this)

  def doesVerificationMatch(verificationHash:String) = Recipient.doesVerificationMatch(this,verificationHash)

  def isEmailVerified = Recipient.isEmailVerified(this)

  def setEmailAsVerified = Recipient.setEmailAsVerified(this)
*/
  def canEdit(wishlist:Wishlist) = {
      isAdmin || wishlist.recipient == this || wishlist.isOrganiser(this)
  }
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

    def authenticate(username: String, password: String) : Option[Recipient]  = {
        findAuthenticationDetailsByUsername(username) match {
            case Some(recipient) =>
                if(BCrypt.checkpw(password,recipient.password.get)){
                    findByUsername(username)
                } else {
                    None
                }
            case None => None
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

  def isEmailVerified(recipient:Recipient) : Boolean = {
    DB.withConnection { implicit connection =>
      SQL(
        """
              SELECT count(*) = 1 FROM emailverification
              WHERE recipientid = {recipientid}
              AND verified = true
        """
      ).on(
        'recipientid -> recipient.recipientId
      ).as(scalar[Boolean].single)
    }
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
