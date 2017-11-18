package models

import scala.concurrent.{ExecutionContext, Future}
import repositories._

case class Wish(
    wishId: Option[Long],
    title: String,
    description: Option[String],
   //  wishEntries:Set[WishEntry] = Set.empty,
    links: Seq[WishLink] = Seq.empty,
    reservation: Option[Reservation] = None,
    recipient: Recipient
) {

   def this(title: String, recipient: Recipient) = this(None, title, None, Seq.empty, None, recipient)

   def this(wishId: Long, recipient: Recipient) = this(Some(wishId), "", None, Seq.empty, None, recipient)

   def this(wishId: Long, title: String, recipient: Recipient) = this(Some(wishId), title, None, Seq.empty, None, recipient)

   def this(wishId: Long, title: String, description: Option[String], recipient: Recipient) = this(Some(wishId), title, description, Seq.empty, None, recipient)

   def this(wishId: Long, title: String, description: Option[String], reservation: Option[Reservation], recipient: Recipient) =
      this(Some(wishId), title, description, Seq.empty, reservation, recipient)

   def save(implicit wishRepository: WishRepository, executionContext: ExecutionContext) =
      wishRepository.saveWish(this)

   def addToWishlist(wishlist: Wishlist)(implicit wishEntryRepository: WishEntryRepository, executionContext: ExecutionContext) =
      WishEntry(this, wishlist).save.map( _ => this)

   def reserve(reserver: Recipient)(implicit reservationRepository: ReservationRepository, executionContext: ExecutionContext) =
      new Reservation(reserver, this).save

   def delete(implicit wishRepository: WishRepository, wishLinkRepository: WishLinkRepository, wishEntryRepository: WishEntryRepository, reservationRepository: ReservationRepository, executionContext: ExecutionContext): Future[Boolean] =
       for {
          _ <- cancelingReservation
          _ <- removeLinks
          _ <- deleteWishEntries
          success <- wishRepository.deleteWish(this)
       } yield success

   private def cancelingReservation()(implicit wishRepository: WishRepository, reservationRepository: ReservationRepository, executionContext: ExecutionContext) =
      reservation.fold(Future.successful(()))( r => r.cancel )

   private def removeLinks(implicit wishLinkRepository: WishLinkRepository, executionContext: ExecutionContext): Future[Boolean] =
      findLinks flatMap { links: List[WishLink] =>
         Future.sequence {
            links map ( _.delete )
         }
      } map ( _ => true )

   private def deleteWishEntries(implicit wishEntryRepository: WishEntryRepository, executionContext: ExecutionContext): Future[Boolean] =
       wishEntryRepository.removeWishFromAllWishlists(this)

   def update(implicit wishRepository: WishRepository, executionContext: ExecutionContext) =
       wishRepository.updateWish(this)
/*
   def removeFromWishlist(wishlist: Wishlist)(implicit wishEntryRepository: WishEntryRepository, wishLinkRepository: WishLinkRepository, wishRepository: WishRepository, reservationRepository: ReservationRepository) =
      wishlist.removeWish(this) flatMap { _ =>
         delete
      }
*/
   def addLink(url: String)(implicit wishLinkRepository: WishLinkRepository, executionContext: ExecutionContext) =
       wishLinkRepository.addLinkToWish(this,url)

   def findLink(linkId: Long)(implicit wishLinkRepository: WishLinkRepository, executionContext: ExecutionContext): Future[Option[WishLink]] =
      wishLinkRepository.findLink(this,linkId)

   def findLinks(implicit wishLinkRepository: WishLinkRepository, executionContext: ExecutionContext): Future[List[WishLink]] =
      wishLinkRepository.findLinks(this)

   def moveToWishlist(targetWishlist: Wishlist)(implicit wishRepository: WishRepository,
            wishEntryRepository: WishEntryRepository, reservationRepository: ReservationRepository,
            executionContext: ExecutionContext) =
      reservation.fold(Future.successful(()))( _.cancel )
         .flatMap { _ =>
            wishEntryRepository.moveWishToWishlist(this, targetWishlist)
         }
}


case class WishLink( linkId : Long, wish : Wish, url : String ){

   def delete(implicit wishLinkRepository: WishLinkRepository, executionContext: ExecutionContext) =
      wishLinkRepository.deleteLink(this)

}


case class WishEntry(
        wish:Wish,
        wishlist: Wishlist,
        ordinal: Option[Int] = None
) {
   def save(implicit wishEntryRepository: WishEntryRepository, executionContext: ExecutionContext) = wishEntryRepository.saveWishEntry(this)

  def updateOrdinal(implicit wishEntryRepository: WishEntryRepository, executionContext: ExecutionContext): Future[WishEntry] =
      wishEntryRepository.update(this)

   require(wish != null && wishlist != null && wish.wishId.isDefined && wishlist.wishlistId.isDefined)

}
