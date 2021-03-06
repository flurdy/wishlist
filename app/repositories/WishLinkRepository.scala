package repositories

import anorm._
import anorm.SqlParser._
import com.google.inject.ImplementedBy
//import java.sql.Connection
import javax.inject.{Inject, Singleton}
import play.api.db._
import scala.concurrent.{ExecutionContext, Future}
import models._
import controllers.WithLogging


trait WishLinkMapper {

   def mapToWishLink(wish: Wish) = {
      get[Long]("linkid") ~
      get[Long]("wishid") ~
      get[String]("url") map {
         case linkId~wishId~url =>
            WishLink( linkId, wish, url)
      }
   }

}


@ImplementedBy(classOf[DefaultWishLinkRepository])
trait WishLinkRepository extends Repository with WishLinkMapper with WithLogging {

   def addLinkToWish(wish: Wish, url: String)(implicit executionContext: ExecutionContext) =
      Future {
         wish.wishId.fold {
            throw new IllegalArgumentException("Can not save wish without id")
         } { wishId =>
            db.withConnection { implicit connection =>
               val nextId = generateNextId("wishlink_seq")
               SQL"""
                     insert into wishlink
                     (linkid,wishid,url)
                     values
                     ($nextId, $wishId, $url)
                 """
                     .executeInsert()
               //               nextId
               WishLink( nextId, wish, url)
            }
         }
      }

   def findLink(wish: Wish, linkId: Long)(implicit executionContext: ExecutionContext): Future[Option[WishLink]] =
      Future {
         wish.wishId.fold{
            throw new IllegalArgumentException("No wish id")
         } { wishId =>
            db.withConnection { implicit connection =>
               SQL"""
                     select * from wishlink
                     where wishid = $wishId
                     and linkid = $linkId
                  """
                  .as(mapToWishLink(wish).singleOpt)
            }
         }
      }

   def findLinks(wish: Wish)(implicit executionContext: ExecutionContext): Future[List[WishLink]] =
      Future {
         wish.wishId.fold{
            throw new IllegalArgumentException("No wish id")
         } { wishId =>
            db.withConnection { implicit connection =>
               SQL"""
                     select * from wishlink
                     where wishid = $wishId
                  """
                  .as(mapToWishLink(wish) *)
            }
         }
      }

   def deleteLink(wishLink: WishLink)(implicit executionContext: ExecutionContext) =
      Future {
         wishLink.wish.wishId.fold{
            throw new IllegalArgumentException("No wish id")
         } { wishId =>
            db.withConnection { implicit connection =>
               SQL"""
                     delete from wishlink
                     where wishid = $wishId
                     and linkid = ${wishLink.linkId}
                  """
                  .execute()
            }
         }
      }
}


@Singleton
class DefaultWishLinkRepository @Inject() (val dbApi: DBApi) extends WishLinkRepository
