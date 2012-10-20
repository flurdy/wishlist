package models


import play.api.Play.current
import org.mindrot.jbcrypt.BCrypt
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.Logger
import java.math.BigInteger
import java.security.{SecureRandom, MessageDigest}

case class Recipient (
    recipientId: Option[Long],
    username: String,
    fullname: Option[String],
    email: String,
    password: Option[String]
){

  def save = Recipient.save(this)

  def delete = Recipient.delete(this)

  def update = Recipient.update(this)

  def resetPassword = Recipient.resetPassword(this)

}

object Recipient {

  val simple = {
    get[Long]("recipientid") ~
      get[String]("username") ~
      get[Option[String]]("fullname") ~
      get[String]("email")  map {
      case recipientid~username~fullname~email=> Recipient( Some(recipientid), username, fullname, email, None)
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
                          (recipientid,username,fullname,email,password) 
                          values 
                          ({recipientid},{username},{fullname},{email},{password})
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
    Wishlist.findWishlistsByUsername(recipient.username).map { wishlist => 
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



  def update(recipient:Recipient) = {
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



  def updatePassword(recipient:Recipient) = {
    Logger.debug("Updating password for recipient: "+recipient.username)
    DB.withConnection { implicit connection =>
      SQL(
        """
            update recipient
            set password = {fullname},
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



  def generateRandomPassword = {
    val source = new BigInteger(130,  new SecureRandom()).toString(32);
    source.substring(0,3)+"-"+source.substring(4,7)+"-"+source.substring(8,11)+"-"+source.substring(12,15)
  }

  def resetPassword(recipient:Recipient) : String = {
      val newPassword = generateRandomPassword
      updatePassword( recipient.copy(password=Option(newPassword)) )
      newPassword
  }



}

