package models

import com.github.t3hnar.bcrypt._
import play.Logger
import play.api.libs.concurrent.Execution.Implicits._
import java.math.BigInteger
import java.security.SecureRandom
import scala.concurrent.Future
import repositories._
import controllers.WithLogging


case class Recipient (
      recipientId: Option[Long],
      username: String,
      fullname: Option[String],
      email: String,
      password: Option[String],
      isAdmin: Boolean=false
) extends WithLogging {

   def this(recipientId: Long) = this(Some(recipientId), "", None, "", None, false)

   def this(username: String) = this(None, username, None, "", None, false)

   def save()(implicit recipientRepository: RecipientRepository): Future[Recipient] =
      recipientRepository.saveRecipient(this)

   def authenticate(possiblePassword: String)(implicit recipientRepository: RecipientRepository) =
      recipientRepository.findCredentials(this).map {
         case Some(passwordFound) => possiblePassword.isBcrypted(passwordFound)
         case _ => false
      }

   def isVerified(implicit recipientRepository: RecipientRepository) =
      recipientRepository.isEmailVerified(this)

   private def findWishlists(implicit wishlistRepository: WishlistRepository): Future[List[Wishlist]] =
      wishlistRepository.findRecipientWishlists(this)

   def findAndInflateWishlists(implicit wishlistRepository: WishlistRepository, recipientRepository: RecipientRepository): Future[List[Wishlist]] =
      findWishlists.flatMap { wishlists =>
         Future.sequence {
            wishlists.map ( _.inflate )
         }
      }

   def findOrGenerateVerificationHash(implicit recipientRepository: RecipientRepository): Future[String] = findVerificationHash.flatMap {
      case Some(verificationHash) => Future.successful( verificationHash )
      case _ =>  generateVerificationHash
   }

   private def findVerificationHash(implicit recipientRepository: RecipientRepository): Future[Option[String]] =
      recipientRepository.findVerificationHash(this)

   private def generateVerificationHash(implicit recipientRepository: RecipientRepository): Future[String] = {
      val verificationHash = new BigInteger(130,  new SecureRandom()).toString(32)
      recipientRepository.saveVerificationHash(this, verificationHash)
   }

   def doesVerificationMatch(verificationHash: String)(implicit recipientRepository: RecipientRepository) =
      recipientRepository.doesVerificationMatch( this, verificationHash)

   def setEmailAsVerified(verificationHash: String)(implicit recipientRepository: RecipientRepository): Future[Boolean] =
      recipientRepository.setEmailAsVerified(this, verificationHash)

   def inflate(implicit recipientRepository: RecipientRepository): Future[Recipient] =
      recipientId.fold{
         recipientRepository.findRecipient(username)
      }{ id =>
         recipientRepository.findRecipientById(id)
      }.map{
         _.getOrElse(throw new IllegalStateException(s"Recipient [$username][$recipientId] not found"))
      }

   private def findOrganisedWishlists(implicit wishlistRepository: WishlistRepository) =
      wishlistRepository.findOrganisedWishlists(this)

   def findAndInflateOrganisedWishlists(implicit wishlistRepository: WishlistRepository, recipientRepository: RecipientRepository): Future[List[Wishlist]] =
      findOrganisedWishlists.flatMap { wishlists =>
         Future.sequence {
            wishlists.map ( _.inflate )
         }
      }

   private def findReservations(implicit reservationRepository: ReservationRepository) =
      reservationRepository.findReservationsByReserver(this)

   def findAndInflateReservations(implicit reservationRepository: ReservationRepository, recipientRepository: RecipientRepository) =
      for {
         shallowReservations <- findReservations
         withReservers       <- reservationRepository.inflateReservationsReserver(shallowReservations)
         withWishRecipients  <- reservationRepository.inflateReservationsWishRecipient(withReservers)
      } yield withWishRecipients

   def update()(implicit recipientRepository: RecipientRepository): Future[Recipient] =
     recipientRepository.updateRecipient(this)

   def updatePassword(newPassword: String)(implicit recipientRepository: RecipientRepository): Future[Recipient] =
      recipientRepository.updatePassword(this.copy(password = Some(newPassword.bcrypt)))

   private def generateRandomPassword = {
      val source = new BigInteger(130,  new SecureRandom()).toString(32)
      source.substring(0,3)+"-"+source.substring(4,7)+"-"+source.substring(8,11)+"-"+source.substring(12,15)
   }

   def resetPassword()(implicit recipientRepository: RecipientRepository) : Future[(Recipient, String)] = {
      val newPassword = generateRandomPassword
      updatePassword( generateRandomPassword ) map ( (_, newPassword ) )
   }

   def delete()(implicit recipientRepository: RecipientRepository,
                         wishlistRepository: WishlistRepository,
                         wishRepository: WishRepository,
                         wishLinkRepository: WishLinkRepository,
                         wishEntryRepository: WishEntryRepository,
                         wishLookup: WishLookup,
                         wishOrganiserRepository: WishlistOrganiserRepository,
                         reservationRepository: ReservationRepository): Future[Boolean] = {

      val cancelingReservations: Future[List[Unit]] =
         findReservations flatMap { reservations =>
            Future.sequence{
               reservations.map { reservation =>
                  reservation.cancel
               }
            }
         }

      val deletingWishlists: Future[List[Boolean]] =
         findWishlists flatMap { wishlists =>
            Future.sequence{
               wishlists map { wishlist =>
                  wishlist.delete
               }
            }
         }

      val removeOrganiserFromWishlists: Future[List[Future[Boolean]]] =
         findOrganisedWishlists flatMap { wishlists =>
            Future.sequence{
               wishlists map { wishlist =>
                  wishlist.findOrganisers map { organisers =>
                     wishlist.removeOrganiser(this)
                  }
               }
            }
         }

      for {
         _ <- cancelingReservations
         _ <- deletingWishlists
         _ <- removeOrganiserFromWishlists.flatMap(Future.sequence(_))
         _ <- recipientRepository.deleteRecipient(this)
      } yield true
   }

  // def resetPassword = Recipient.resetPassword(this)

  def isSame(other: Recipient) = isSameId(other) || isSameUsername(other)

  def isSameId(other: Recipient) = recipientId.fold(false)( _ => recipientId == other.recipientId )

  def isSameUsername(other: Recipient) = username == other.username

  def canEdit(wishlist: Wishlist)(implicit wishlistLookup: WishlistLookup) =
     if(isAdmin || wishlist.recipient.isSame(this)) Future.successful(true)
     else wishlist.isOrganiser(this)

  def findEditableWishlists(implicit wishlistRepository: WishlistRepository) =
     for {
        ownWishlists       <- findWishlists
        organisedWishlists <- findOrganisedWishlists
     } yield ownWishlists union organisedWishlists
}



/*
object Recipient {

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
}
*/
