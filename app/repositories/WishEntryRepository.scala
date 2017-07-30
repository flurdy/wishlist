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


@ImplementedBy(classOf[DefaultWishEntryRepository])
trait WishEntryRepository extends Repository with WithLogging {

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
         logger.debug("Deleting wishentry: "+wish.title)
         (wish.wishId, wishlist.wishlistId) match {
         case (Some(wishId), Some(wishlistId)) =>
            db.withConnection { implicit connection =>
               SQL"""
                        delete from wishentry
                        where wishid = $wishId
                        and wishlistid = $wishlistId
                     """
                     .execute()
            }
         case _ =>
            throw new IllegalArgumentException("Can not remove wish to wishlist without ids")
         }
      }.map {
         if(_) wishlist
         else throw new IllegalStateException("Unable to remove wish from wishlist")
      }
}


@Singleton
class DefaultWishEntryRepository @Inject() (val dbApi: DBApi) extends WishEntryRepository
