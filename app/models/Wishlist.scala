package models

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import repositories._

case class Wishlist(
    wishlistId: Option[Long],
    title: String,
    description: Option[String],
    recipient: Recipient,
    organisers: Seq[Recipient] = Seq.empty
) {

    def this(wishlistId: Long, recipient: Recipient) = this(Some(wishlistId),"",None, recipient)

    def this(title: String, recipient: Recipient) = this(None, title, None, recipient, Seq.empty)

    def save(implicit wishlistRepository: WishlistRepository) =
       wishlistRepository.saveWishlist(this)

    def delete(implicit wishlistRepository: WishlistRepository) = wishlistRepository.deleteWishlist(this)

    def findWishes(implicit wishLookup: WishLookup, wishLinkRepository: WishLinkRepository): Future[Seq[Wish]] =
      wishLookup.findWishes(this)

    def update(implicit wishlistRepository: WishlistRepository) =
       wishlistRepository.updateWishlist(this)

    def removeWish(wish: Wish)(implicit wishEntryRepository: WishEntryRepository) =
       wishEntryRepository.removeWishFromWishlist(wish, this)


    def findOrganisers(implicit recipientRepository: RecipientRepository) =
       recipientRepository.findOrganisers(this)

    /*

    def addOrganiser(organiser:Recipient) = Wishlist.addOrganiserToWishlist(organiser,this)

    def removeOrganiser(organiser:Recipient) = Wishlist.removeOrganiserFromWishlist(organiser,this)

    */
    def isOrganiser(organiser: Recipient)(implicit wishlistLookup: WishlistLookup) =
      wishlistLookup.isOrganiserOfWishlist(organiser, this)

    def inflate(implicit recipientRepository: RecipientRepository): Future[Wishlist] =
       recipient.recipientId.fold{
          throw new IllegalStateException("No recipient id")
       }{ recipientId =>
          recipientRepository.findRecipientById(recipientId) map {
             _.fold(throw new IllegalStateException("No recipient found")){ r =>
                this.copy(recipient = r)
             }
          }
       }

    require(recipient != null)
}
/*
object Wishlist {

  val simple = {
    get[Long]("wishlistid") ~
      get[String]("title") ~
      get[Option[String]]("description") ~
      get[Long]("recipientid") map {
      case wishlistid~title~description~recipientid=> {
        Recipient.findById(recipientid) match {
            case Some(recipient) => Wishlist( Some(wishlistid), title, description, recipient)
            case None => {
                Logger.error("Wishlist %d recipient %d not found".format(wishlistid,recipientid))
                null
            }
        }
      }
    }
  }

    def delete(wishlist:Wishlist) {
        Logger.debug("Deleting wishlist: "+wishlist.title)
        DB.withConnection { implicit connection =>
          SQL(
            """
                    delete from wish
                    where wishid in (select wishid from wishentry where wishlistid = {wishlistid})
            """
          ).on(
            'wishlistid -> wishlist.wishlistId
          ).execute()
          SQL(
            """
                    delete from wishentry
                    where wishlistid = {wishlistid}
            """
          ).on(
            'wishlistid -> wishlist.wishlistId
          ).execute()
            SQL(
                """
                    delete from wishlist
                    where wishlistid = {wishlistid}
                """
            ).on(
                'wishlistid -> wishlist.wishlistId
            ).execute()
        }
    }



    def findAll : Seq[Wishlist] = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                  SELECT * FROM wishlist
                  ORDER BY recipientid DESC,title
                """
            ).as(Wishlist.simple *)
        }
    }

    def searchForWishlistsContaining(searchTerm:String) : Seq[Wishlist] = {
        Logger.debug("Search term is " + searchTerm)
        val searchLikeTerm = "%" + searchTerm + "%"
        DB.withConnection { implicit connection =>
            SQL(
                """
                  SELECT * FROM wishlist
                  where title like {term}
                  ORDER BY recipientid DESC, title
                """
            ).on(
                'term -> searchLikeTerm
            ).as(Wishlist.simple *)
        }
    }


    def findWishesForWishlist(wishlist:Wishlist) : Seq[Wish] = {
        DB.withConnection { implicit connection =>
            val wishes = SQL(
              """
                  SELECT wi.*,rec.username
                  FROM wish wi
                  INNER JOIN wishentry we on wi.wishid = we.wishid
                  INNER JOIN recipient rec on rec.recipientid = wi.recipientid
                  where we.wishlistid = {wishlistid}
                  ORDER BY we.ordinal,wi.title DESC
              """
            ).on(
                'wishlistid -> wishlist.wishlistId
            ).as(Wish.simple *)
            wishes.map { wish =>
              wish.copy(links=wish.findLinks)
            }
        }
    }


    def findByRecipient(recipient:Recipient) : Seq[Wishlist] = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                  SELECT * FROM wishlist
                  where recipientid = {recipientid}
                  ORDER BY title DESC
                """
            ).on(
                'recipientid -> recipient.recipientId.get
            ).as(Wishlist.simple *)
        }
    }


    def findByOrganiser(organiser:Recipient) : Seq[Wishlist] = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                  SELECT wi.* FROM wishlist wi
                  INNER JOIN wishlistorganiser wo on wo.wishlistid = wi.wishlistid
                  WHERE wo.recipientid = {recipientid}
                  ORDER BY wi.recipientid,wi.title DESC
                """
            ).on(
                'recipientid -> organiser.recipientId.get
            ).as(Wishlist.simple *)
        }
    }




  def addOrganiserToWishlist(organiser:Recipient,wishlist:Wishlist) {
    DB.withConnection { implicit connection =>
      SQL(
        """
            insert into wishlistorganiser
            (wishlistid,recipientid)
            values
            ({wishlistid},{recipientid})
        """
      ).on(
        'wishlistid -> wishlist.wishlistId.get,
        'recipientid -> organiser.recipientId.get
      ).executeInsert()
    }
  }



  def removeOrganiserFromWishlist(organiser:Recipient,wishlist:Wishlist) {
    DB.withConnection { implicit connection =>
      SQL(
        """
            delete from wishlistorganiser
            where wishlistid = {wishlistid}
            and recipientid = {recipientid}
        """
      ).on(
        'wishlistid -> wishlist.wishlistId.get,
        'recipientid -> organiser.recipientId.get
      ).execute()
    }
  }


}

*/
