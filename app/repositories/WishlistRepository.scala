package repositories

import anorm._
import anorm.SqlParser._
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.db._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import controllers.WithLogging


trait WishlistMapper {

  val MapToShallowWishlist = {
      get[Long]("wishlistid") ~
      get[String]("title") ~
      get[Option[String]]("description") ~
      get[Long]("recipientid") map {
         case wishlistid~title~description~recipientId => {
            Wishlist( Some(wishlistid), title, description, new Recipient(recipientId))
         }
      }
   }
}

@ImplementedBy(classOf[DefaultWishlistLookup])
trait WishlistLookup {

   def wishlistRepository: WishlistRepository
   // def wishRepository: WishRepository

   def findRecipientWishlists(recipient: Recipient) = wishlistRepository.findRecipientWishlists(recipient)

   def findWishlist(wishlistId: Long) = wishlistRepository.findWishlist(wishlistId)

   def isOrganiserOfWishlist(organiser: Recipient, wishlist: Wishlist) =
      wishlistRepository.isOrganiserOfWishlist(organiser, wishlist)

   // def findWishes(wishlist: Wishlist): Future[Seq[Wish]] = wishLookup.findWishes(wishlist)

}

@Singleton
class DefaultWishlistLookup @Inject() (val wishlistRepository: WishlistRepository, val wishRepository: WishRepository) extends WishlistLookup


@ImplementedBy(classOf[DefaultWishlistRepository])
trait WishlistRepository extends Repository with WishlistMapper with WithLogging  {

   def findWishlist(wishlistId: Long): Future[Option[Wishlist]] =
      Future {
         db.withConnection { implicit connection =>
            SQL"""
                  select *
                  from wishlist
                  where wishlistid = $wishlistId
               """
            .as(MapToShallowWishlist.singleOpt)
         }
      }

   def findRecipientWishlists(recipient: Recipient): Future[List[Wishlist]] =
      Future {
         recipient.recipientId.fold{
            throw new IllegalStateException("No recipient id")
         } { recipientId =>
            db.withConnection { implicit connection =>
               SQL"""
                     SELECT * FROM wishlist
                     where recipientid = $recipientId
                     ORDER BY title DESC
                  """
               .as(MapToShallowWishlist *)
            }
         }
      }

   def findOrganisedWishlists(organiser: Recipient): Future[List[Wishlist]] =
      Future {
         organiser.recipientId.fold{
            throw new IllegalStateException("No recipient id")
         } { organiserId =>
            db.withConnection { implicit connection =>
               SQL"""
                     SELECT wi.* FROM wishlist wi
                     INNER JOIN wishlistorganiser wo on wo.wishlistid = wi.wishlistid
                     WHERE wo.recipientid = $organiserId
                     ORDER BY wi.recipientid,wi.title DESC
                  """
               .as(MapToShallowWishlist *)
            }
         }
      }

   def saveWishlist(wishlist: Wishlist): Future[Either[_,Wishlist]] =
      Future {
         wishlist.recipient.recipientId.flatMap{ recipientId =>
            db.withConnection{ implicit connection =>
               logger.debug(s"Saving new wishlist: ${wishlist.title}")
               val nextId = SQL("SELECT NEXTVAL('wishlist_seq')").as(scalar[Long].single)
               SQL"""
                     insert into wishlist
                     (wishlistid,title,description,recipientid)
                     values
                     ($nextId, ${wishlist.title}, ${wishlist.description}, ${recipientId})
                  """
               .executeInsert()
               .map{ wishlistId =>
                  wishlist.copy(wishlistId = Some(wishlistId))
               }
            }
         }.toRight(new IllegalStateException("Saving wishlist failed"))
      }

   def deleteWishlist(wishlist: Wishlist): Future[Either[Throwable,Boolean]] =
      Future {
         wishlist.wishlistId.fold[Either[Throwable,Boolean]] {
            Left(new IllegalArgumentException("Can not delete wishlist without an id"))
         } { wishlistId =>
            db.withConnection{ implicit connection =>
               logger.info(s"Deleting wishlist [${wishlist.wishlistId}] for [${wishlist.recipient.username}]")
               SQL"""
                     delete from wish
                     where wishid in
                        (select wishid
                        from wishentry
                        where wishlistid = $wishlistId)
                  """
               .execute()
               SQL"""
                     delete from wishentry
                     where wishlistid = $wishlistId
                  """
               .execute()
               SQL"""
                     delete from wishlist
                     where wishlistid = $wishlistId
                  """
               .executeUpdate() match {
                  case 0 => Right(false)
                  case _ => Right(true)
               }
            }
         }
      }

   def isOrganiserOfWishlist(organiser: Recipient, wishlist: Wishlist) =
      Future {
         organiser.recipientId.fold(false){ recipientId =>
            wishlist.wishlistId.fold(false){  wishlistId =>
               db.withConnection { implicit connection =>
                  SQL"""
                        SELECT count(*) = 1 FROM wishlistorganiser
                        WHERE recipientid = $recipientId
                        AND wishlistid = $wishlistId
                     """
                  .as(scalar[Boolean].single)
               }
            }
         }
      }

   def findAll: Future[List[Wishlist]] =
      Future{
         db.withConnection { implicit connection =>
            SQL"""
               SELECT * FROM wishlist
               ORDER BY recipientid DESC,title
             """
             .as(MapToShallowWishlist *)
         }
      }

   def searchForWishlistsContaining(searchTerm: String): Future[List[Wishlist]] =
      Future{
         db.withConnection { implicit connection =>
            val searchLikeTerm = "%" + searchTerm.toLowerCase.trim + "%"
            SQL"""
               SELECT * FROM wishlist
               where lower(title) like $searchLikeTerm
               ORDER BY recipientid DESC, title
             """
            .as(MapToShallowWishlist *)
         }
      }
}

@Singleton
class DefaultWishlistRepository @Inject() (val dbApi: DBApi) extends WishlistRepository
