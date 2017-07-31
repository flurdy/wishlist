package models

// import anorm._
// import anorm.SqlParser._
// import play.api.Play.current
// import play.api.db.DB
// import play.Logger
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
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

   def save(implicit wishRepository: WishRepository) =
      wishRepository.saveWish(this)

   def addToWishlist(wishlist: Wishlist)(implicit wishEntryRepository: WishEntryRepository) =
      WishEntry(this, wishlist).save.map( _ => this)

   def reserve(recipient:Recipient)(implicit reservationRepository: ReservationRepository) =
      new Reservation(recipient,this).save

//   def unreserve: Future[Reservation] = ???
      // reservation map ( realReservation => realReservation.copy(wish=this).cancel )

   /*
    def this(wishId:Long,
        title:String,
        description:Option[String],
        reservation:Option[Reservation],recipient:Recipient) =
          this(Some(wishId), title, description, Set.empty, Seq.empty, reservation, recipient)

    def this(wishId:Long) = this(Some(wishId),"",None, Set.empty, Seq.empty, None , null)
    def this(wishId:Long,title:String,description:Option[String],recipient:Recipient) = this(Some(wishId),title,description, Set.empty, Seq.empty, None, recipient:Recipient)



    def delete = Wish.delete(this)
*/
    def update(implicit wishRepository: WishRepository) =
       wishRepository.updateWish(this)

   def removeFromWishlist(wishlist: Wishlist)(implicit wishEntryRepository: WishEntryRepository, wishRepository: WishRepository) =
      wishlist.removeWish(this) flatMap { _ =>
         wishRepository.deleteWish(this)
      }

   def addLink(url: String)(implicit wishLinkRepository: WishLinkRepository) =
       wishLinkRepository.addLinkToWish(this,url)

//       def deleteLink(linkId:Long) = Wish.deleteLinkFromWish(this,linkId)

   def findLink(linkId: Long)(implicit wishLinkRepository: WishLinkRepository): Future[Option[WishLink]] =
      wishLinkRepository.findLink(this,linkId)

   def findLinks(implicit wishLinkRepository: WishLinkRepository): List[WishLink] = List() // TODO WishLink.findWishLinks(this)

   def moveToWishlist(targetWishlist: Wishlist)(implicit wishEntryRepository: WishEntryRepository) =
      reservation.fold(Future.successful(()))( _.cancel )
         .flatMap { _ =>
            wishEntryRepository.moveWishToWishlist(this, targetWishlist)
         }
}


case class WishLink(
  linkId : Long,
  wish : Wish,
  url : String
){

   def delete(implicit wishLinkRepository: WishLinkRepository) =
      wishLinkRepository.deleteLink(this)

}

/*

object WishLink {

  val simple = {
    get[Long]("linkid") ~
    get[Long]("wishid") ~
    get[String]("url") map {
      case linkid~wishid~url => {
        WishLink( linkid,  new Wish(wishid), url )
      }
    }
  }


  def findWishLinks(wish:Wish): List[WishLink] = {
    DB.withConnection { implicit connection =>
        SQL(
          """
              SELECT *
              FROM wishlink
              where wishid = {wishid}
              ORDER BY linkid
          """
        ).on(
            'wishid -> wish.wishId.get
        ).as(WishLink.simple *)
    }
  }

}


*/


case class WishEntry(
        wish:Wish,
        wishlist: Wishlist,
        ordinal: Option[Int] = None
) {

   def save(implicit wishEntryRepository: WishEntryRepository) = wishEntryRepository.saveWishEntry(this)

  def updateOrdinal(implicit wishEntryRepository: WishEntryRepository): Future[WishEntry] =
      wishEntryRepository.update(this)

   require(wish != null && wishlist != null && wish.wishId.isDefined && wishlist.wishlistId.isDefined)

}

/*

object WishEntry {


  val simple = {
    get[Long]("wishid") ~
    get[Long]("wishlistid") ~
    get[Option[Int]]("ordinal") ~
    get[String]("title") ~
    get[Option[String]]("description") ~
    get[Option[Long]]("reservationid") ~
    get[Long]("recipientid") map {
      case wishid~wishlistid~ordinal~title~description~reservationid~recipientid => {
       WishEntry(
            new Wish( wishid, title, description, Reservation.create(reservationid), new Recipient(recipientid)),
            new Wishlist(wishlistid),
            ordinal)
      }
    }
  }

}

*/
