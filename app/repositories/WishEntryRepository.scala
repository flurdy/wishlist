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

trait WishEntryMapper {

   val MapToShallowWishEntry = {
      get[Long]("wishid") ~
      get[Long]("wishlistid") ~
      get[Option[Int]]("ordinal") ~
      get[Long]("recipientid") map {
         case wishId~wishlistId~ordinal~recipientId => {
            val recipient = new Recipient(recipientId)
            val wishlist  = new Wishlist(wishlistId, recipient)
            val wish      = new Wish(wishId, recipient)
            WishEntry(wish, wishlist, ordinal)
         }
      }
   }
}

@ImplementedBy(classOf[DefaultWishEntryRepository])
trait WishEntryRepository extends Repository with WishEntryMapper with WithLogging {

   def saveWishEntry(wishEntry: WishEntry): Future[WishEntry] =
      Future{
         (wishEntry.wish.wishId, wishEntry.wishlist.wishlistId) match {
         case (Some(wishId), Some(wishlistId)) =>
            db.withConnection{ implicit connection =>
               val maxOrdinal =
                  SQL"""
                        SELECT COALESCE(MAX(ordinal),0) + 1 from wishentry
                        where wishlistid = $wishlistId
                     """
                     .as(scalar[Int].single)

               SQL"""
                     insert into wishentry
                     (wishid,wishlistid,ordinal)
                     values
                     ($wishId,$wishlistId,$maxOrdinal)
                  """
                  .executeInsert()
               wishEntry
            }
         case _ =>
            throw new IllegalArgumentException("Can not add wish to wishlist without ids")
         }
      }

   def removeWishFromWishlist(wish: Wish, wishlist: Wishlist): Future[Wishlist] =
      Future {
         (wish.wishId, wishlist.wishlistId) match {
         case (Some(wishId), Some(wishlistId)) =>
            db.withConnection { implicit connection =>
               SQL"""
                     delete from wishentry
                     where wishid = $wishId
                     and wishlistid = $wishlistId
                  """
                  .executeUpdate()
            }
         case _ =>
            throw new IllegalArgumentException("Can not remove wish to wishlist without ids")
         }
      }.map { d =>
         if(d > 0) wishlist
         else throw new IllegalStateException("Unable to remove wish from wishlist")
      }


   def removeWishFromAllWishlists(wish: Wish): Future[Boolean] =
      Future {
         logger.debug("Deleting wishentry: "+wish.title)
         wish.wishId.fold {
            throw new IllegalArgumentException("Can not remove wish from wishlists without id")
         }{ wishId =>
            db.withConnection { implicit connection =>
               SQL"""
                        delete from wishentry
                        where wishid = $wishId
                     """
                     .executeUpdate()
            }
         }
      }.map( _ > 0 )

   def findByIds(wishId: Long, wishlistId: Long): Future[Option[WishEntry]]=
      Future {
         db.withConnection { implicit connection =>
            SQL"""
                 SELECT we.wishid, we.wishlistid, we.ordinal, wi.recipientid
                 FROM wishentry we
                 LEFT JOIN wish wi ON wi.wishid = we.wishid
                 WHERE we.wishid = $wishId
                 AND we.wishlistid = $wishlistId
              """
              .as(MapToShallowWishEntry.singleOpt)
         }
      }


   def moveWishToWishlist(wish: Wish, targetWishlist: Wishlist): Future[Wish] =
      (wish.wishId, targetWishlist.wishlistId) match {
         case (Some(wishId), Some(wishlistId)) =>
            Future {
               db.withConnection { implicit connection =>
                  val updated =
                     SQL"""
                        update wishentry
                        set wishlistid = $wishlistId
                        where wishid = $wishId
                    """
                    .executeUpdate()
                  if(updated > 0) wish
                  else throw new IllegalStateException("Unable to update wish entry")
               }
            }
         case _ => throw new IllegalArgumentException("No ids")
      }

   private def updateOrdinal(wishId: Long, wishlistId: Long, ordinal: Int) =
      Future {
         db.withConnection { implicit connection =>
            SQL"""
               update wishentry
               set ordinal = $ordinal
               where wishid = $wishId
               and wishlistid = $wishlistId
           """
           .executeUpdate()
         }
      }


   private def removeOrdinal(wishId: Long, wishlistId: Long) =
      Future {
         db.withConnection { implicit connection =>
            SQL"""
                  update wishentry
                  set ordinal = null
                  where wishid = $wishId
                  and wishlistid = $wishlistId
              """
              .executeUpdate()
         }
      }


   def update(wishEntry: WishEntry): Future[WishEntry] = {
      (wishEntry.wish.wishId, wishEntry.wishlist.wishlistId) match {
         case (Some(wishId), Some(wishlistId)) =>
            wishEntry.ordinal.fold{
               removeOrdinal(wishId, wishlistId)
            }{ ordinal =>
               updateOrdinal(wishId, wishlistId, ordinal)
            }.map { updated =>
               if(updated > 0) wishEntry
               else throw new IllegalStateException("Unable to update wish entry")
            }
         case _ => throw new IllegalArgumentException("Missing ids")
      }
   }
}


@Singleton
class DefaultWishEntryRepository @Inject() (val dbApi: DBApi) extends WishEntryRepository
