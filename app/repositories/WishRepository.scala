package repositories

import anorm._
import anorm.JodaParameterMetaData._
import anorm.SqlParser._
import com.google.inject.ImplementedBy
import java.sql.Connection
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import controllers.WithLogging

trait WishMapper {

   val MapToWish = {
      get[Long]("wishid") ~
      get[String]("title") ~
      get[Option[String]]("description") ~
      get[Option[Long]]("reservationid") ~
      get[Long]("recipientid") ~
      get[Option[Long]]("recipientid") map {
         case wishId~title~description~reservationId~recipientId~reserverId => {
            val recipient   = new Recipient(recipientId)
            val reservation =
               for {
                  _               <- reservationId
                  reserverId2     <- reserverId
                  shallowReserver =  new Recipient(reserverId2)
                  shallowWish     =  new Wish(wishId, recipient)
               } yield Reservation(reservationId, shallowReserver, shallowWish)
            new Wish( wishId, title, description, reservation, recipient)
         }
      }
   }
}

@ImplementedBy(classOf[DefaultWishLookup])
trait WishLookup extends Repository with WishMapper with WithLogging {

   def findWishes(wishlist: Wishlist): Future[Seq[Wish]] =
      Future {
         db.withConnection { implicit connection =>
            wishlist.wishlistId.fold(List[Wish]()){ wishlistId => {
               SQL"""
                     SELECT wi.*, re.recipientid as reserverid
                     FROM wish wi
                     INNER JOIN wishentry we on wi.wishid = we.wishid
                     LEFT JOIN reservation re on wi.wishid = re.wishid
                     where we.wishlistid = $wishlistId
                     ORDER BY we.ordinal,wi.title DESC
                  """
               .as(MapToWish *)
            }
         }
      }
      .map { wish =>
         logger.debug("FOUND " + wish)
         wish.copy(links = wish.findLinks)
      }
   }
   def findWishById(wishId: Long): Future[Option[Wish]] =
      Future {
         db.withConnection { implicit connection =>
            SQL"""
                 SELECT wish.*,recipient.username FROM wish
                 INNER JOIN recipient on recipient.recipientid = wish.recipientid
                 WHERE wish.wishid = $wishId
               """
               .as(MapToWish.singleOpt)
         }
      }
}


@Singleton
class DefaultWishLookup @Inject() (val dbApi: DBApi) extends WishLookup


@ImplementedBy(classOf[DefaultWishRepository])
trait WishRepository extends Repository with WishMapper with WithLogging {

   private def generateNextWishId()(implicit connection: Connection) =
      SQL"""SELECT NEXTVAL('wish_seq')""".as(scalar[Long].single)

   def saveWish(wish: Wish): Future[Wish] =
      Future {
         wish.recipient.recipientId.fold{
            throw new IllegalArgumentException("Can not save wish without recipient")
         }{ recipientId =>
            db.withConnection{ implicit connection =>
               logger.debug("Inserting wish: "+wish.title)
               val nextId = generateNextWishId()
               SQL"""
                    insert into wish
                    (wishid,title,description,recipientid)
                    values
                    ($nextId,${wish.title},${wish.description},$recipientId)
                  """
                  .executeInsert()
               wish.copy(wishId = Some(nextId))
            }
         }
      }

   def updateWish(wish:Wish) = {
      Future {
         wish.wishId.fold{
            throw new IllegalArgumentException("Can not save wish without id")
         }{ wishId =>
            logger.debug("Updating wish: "+wish.wishId)
            logger.debug("Updating wish: "+wish.title)
            db.withConnection { implicit connection =>
               val updated =
                  SQL"""
                     update wish
                     set title = ${wish.title}, description = ${wish.description}
                     where wishid = $wishId
                  """
                  .executeUpdate()
               if(updated > 0 ) wish
               else throw new IllegalStateException("Unable to update wish")
            }
         }
      }
   }
}


@Singleton
class DefaultWishRepository @Inject() (val dbApi: DBApi) extends WishRepository


@ImplementedBy(classOf[DefaultWishEntryRepository])
trait WishEntryRepository extends Repository {

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
               throw new IllegalArgumentException("Can not add wish to wishlist without id")
         }
      }
}


@Singleton
class DefaultWishEntryRepository @Inject() (val dbApi: DBApi) extends WishEntryRepository
