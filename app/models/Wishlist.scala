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

    def delete(implicit wishlistRepository: WishlistRepository,
                        reservationRepository: ReservationRepository,
                        recipientRepository: RecipientRepository,
                        wishOrganiserRepository: WishlistOrganiserRepository,
                        wishLookup: WishLookup,
                        wishLinkRepository: WishLinkRepository,
                        wishRepository: WishRepository,
                        wishEntryRepository: WishEntryRepository): Future[Boolean] = {
      for {
         _       <- deleteAllWishes
         _       <- removeAllOrganisers
         success <- wishlistRepository.deleteWishlist(this)
      } yield success
    }

    private def deleteAllWishes(implicit wishLookup: WishLookup,
           wishRepository: WishRepository,
           wishLinkRepository: WishLinkRepository,
           wishEntryRepository: WishEntryRepository): Future[Boolean] = {
      findWishes flatMap { wishes =>
         Future.sequence {
            wishes map { wish =>
               wish.delete
            }
         }.map{ _ => true }
      }
   }

    private def removeAllOrganisers
    (implicit recipientRepository: RecipientRepository,
              wishOrganiserRepository: WishlistOrganiserRepository)
              : Future[Boolean] = {
      findOrganisers flatMap { organisers =>
         Future.sequence {
            organisers map ( removeOrganiser(_) )
         }.map{ _ => true }
      }
   }

    def findWishes(implicit wishLookup: WishLookup, wishLinkRepository: WishLinkRepository): Future[Seq[Wish]] =
      wishLookup.findWishes(this)

    def update(implicit wishlistRepository: WishlistRepository) =
       wishlistRepository.updateWishlist(this)

    def removeWish(wish: Wish)(implicit wishEntryRepository: WishEntryRepository) =
       wishEntryRepository.removeWishFromWishlist(wish, this)

    def findOrganisers(implicit recipientRepository: RecipientRepository) =
       recipientRepository.findOrganisers(this)

    def addOrganiser(organiser: Recipient)(implicit wishOrganiserRepository: WishlistOrganiserRepository): Future[Wishlist] =
       wishOrganiserRepository.addOrganiserToWishlist(organiser,this)

    def removeOrganiser(organiser: Recipient)(implicit wishOrganiserRepository: WishlistOrganiserRepository) =
       wishOrganiserRepository.removeOrganiserFromWishlist(organiser,this)

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


}

*/
