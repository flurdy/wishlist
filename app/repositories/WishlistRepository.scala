package repositories

import anorm._
import anorm.JodaParameterMetaData._
import anorm.SqlParser._
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._


trait WishlistMapper {

  val MapToShallowWishlist = {
      get[Long]("wishlistid") ~
      get[String]("title") ~
      get[Option[String]]("description") ~
      get[Long]("recipientid") map {
         case wishlistid~title~description~recipientId=> {
            Wishlist( Some(wishlistid), title, description, new Recipient(recipientId))
         }
      }
   }
}

@ImplementedBy(classOf[DefaultWishlistLookup])
trait WishlistLookup {

   def wishlistRepository: WishlistRepository

   def findRecipientWishlists(recipient: Recipient) = wishlistRepository.findRecipientWishlists(recipient)

   def findWishlist(wishlistId: Long) = wishlistRepository.findWishlist(wishlistId)

}

@Singleton
class DefaultWishlistLookup @Inject() (val wishlistRepository: WishlistRepository) extends WishlistLookup


@ImplementedBy(classOf[DefaultWishlistRepository])
trait WishlistRepository extends Repository with WishlistMapper {

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
         recipient.recipientId.fold(List[Wishlist]()){ recipientId =>
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

   def saveWishlist(wishlist: Wishlist): Future[Either[_,Wishlist]] =
      Future {
         wishlist.recipient.recipientId.flatMap{ recipientId =>
            db.withConnection{ implicit connection =>
               Logger.debug(s"Saving new wishlist: ${wishlist.title}")
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
               Logger.info(s"Deleting wishlist [${wishlist.wishlistId}] for [${wishlist.recipient.username}]")
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
}

@Singleton
class DefaultWishlistRepository @Inject() (val dbApi: DBApi) extends WishlistRepository
