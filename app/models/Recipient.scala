package models

import com.github.t3hnar.bcrypt._
import java.math.BigInteger
import java.security.SecureRandom
import scala.concurrent.{ExecutionContext, Future}
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

   def save()(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[Recipient] =
      recipientRepository.saveRecipient(this)

   def authenticate(possiblePassword: String)(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext) =
      recipientRepository.findCredentials(this).map {
         case Some(passwordFound) => possiblePassword.isBcrypted(passwordFound)
         case _ => false
      }

   def isVerified(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext) =
      recipientRepository.isEmailVerified(this)

   private def findWishlists(implicit wishlistRepository: WishlistRepository, executionContext: ExecutionContext): Future[List[Wishlist]] =
      wishlistRepository.findRecipientWishlists(this)

   def findAndInflateWishlists(implicit wishlistRepository: WishlistRepository, recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[List[Wishlist]] =
      findWishlists.flatMap { wishlists =>
         Future.sequence {
            wishlists.map ( _.inflate )
         }
      }

   def findOrGenerateVerificationHash(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[String] = findVerificationHash.flatMap {
      case Some(verificationHash) => Future.successful( verificationHash )
      case _ =>  generateVerificationHash
   }

   private def findVerificationHash(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[Option[String]] =
      recipientRepository.findVerificationHash(this)

   private def generateVerificationHash(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[String] = {
      val verificationHash = new BigInteger(130,  new SecureRandom()).toString(32)
      recipientRepository.saveVerificationHash(this, verificationHash)
   }

   def doesVerificationMatch(verificationHash: String)(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext) =
      recipientRepository.doesVerificationMatch( this, verificationHash)

   def setEmailAsVerified(verificationHash: String)(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[Boolean] =
      recipientRepository.setEmailAsVerified(this, verificationHash)

   def inflate(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[Recipient] =
      recipientId.fold{
         recipientRepository.findRecipient(username)
      }{ id =>
         recipientRepository.findRecipientById(id)
      }.map{
         _.getOrElse(throw new IllegalStateException(s"Recipient [$username][$recipientId] not found"))
      }

   private def findOrganisedWishlists(implicit wishlistRepository: WishlistRepository, executionContext: ExecutionContext) =
      wishlistRepository.findOrganisedWishlists(this)

   def findAndInflateOrganisedWishlists(implicit wishlistRepository: WishlistRepository, recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[List[Wishlist]] =
      findOrganisedWishlists.flatMap { wishlists =>
         Future.sequence {
            wishlists.map ( _.inflate )
         }
      }

   private def findReservations(implicit reservationRepository: ReservationRepository, executionContext: ExecutionContext) =
      reservationRepository.findReservationsByReserver(this)

   def findAndInflateReservations(implicit reservationRepository: ReservationRepository, recipientRepository: RecipientRepository, executionContext: ExecutionContext) =
      for {
         shallowReservations <- findReservations
         withReservers       <- reservationRepository.inflateReservationsReserver(shallowReservations)
         withWishRecipients  <- reservationRepository.inflateReservationsWishRecipient(withReservers)
      } yield withWishRecipients

   def update()(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[Recipient] =
     recipientRepository.updateRecipient(this)

   def updatePassword(newPassword: String)(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext): Future[Recipient] =
      recipientRepository.updatePassword(this.copy(password = Some(newPassword.bcrypt)))

   private def generateRandomPassword = {
      val source = new BigInteger(130,  new SecureRandom()).toString(32)
      source.substring(0,3)+"-"+source.substring(4,7)+"-"+source.substring(8,11)+"-"+source.substring(12,15)
   }

   def resetPassword()(implicit recipientRepository: RecipientRepository, executionContext: ExecutionContext) : Future[(Recipient, String)] = {
      val newPassword = generateRandomPassword
      updatePassword( generateRandomPassword ) map ( (_, newPassword ) )
   }

   def delete()(implicit recipientRepository: RecipientRepository,
                         wishlistRepository: WishlistRepository,
                         wishRepository: WishRepository,
                         wishLinkRepository: WishLinkRepository,
                         wishEntryRepository: WishEntryRepository,
                         wishLookup: WishLookup,
                         wishlistOrganiserRepository: WishlistOrganiserRepository,
                         reservationRepository: ReservationRepository,
                         executionContext: ExecutionContext): Future[Boolean] = {

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

  def isSame(other: Recipient) = isSameId(other) || isSameUsername(other)

  def isSameId(other: Recipient) = recipientId.fold(false)( _ => recipientId == other.recipientId )

  def isSameUsername(other: Recipient) = username == other.username

  def canEdit(wishlist: Wishlist)(implicit wishlistLookup: WishlistLookup, executionContext: ExecutionContext) =
     if(isAdmin || wishlist.recipient.isSame(this)) Future.successful(true)
     else wishlist.isOrganiser(this)

  def findEditableWishlists(implicit wishlistRepository: WishlistRepository, executionContext: ExecutionContext) =
     for {
        ownWishlists       <- findWishlists
        organisedWishlists <- findOrganisedWishlists
     } yield ownWishlists union organisedWishlists
}
