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

   def this(wishId: Long, title: String, description: Option[String], reservation: Option[Reservation], recipient: Recipient) =
      this(Some(wishId), title, description, Seq.empty, reservation, recipient)

   def save(implicit wishRepository: WishRepository) =
      wishRepository.saveWish(this)

   def addToWishlist(wishlist: Wishlist)(implicit wishEntryRepository: WishEntryRepository) =
      WishEntry(this, wishlist).save.map( _ => this)

   def reserve(recipient:Recipient)(implicit reservationRepository: ReservationRepository) =
      new Reservation(recipient,this).save

   def unreserve: Future[Either[_,Reservation]] = ???
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

    def update = Wish.update(this)




    def addLink(url:String) = Wish.addLinkToWish(this,url)

    def deleteLink(linkId:Long) = Wish.deleteLinkFromWish(this,linkId)

    def findLink(linkId:Long) : Option[String] = Wish.findLink(this,linkId)
    */

    def findLinks : List[WishLink] = List() // WishLink.findWishLinks(this)

   //  def moveToWishlist(targetWishlist:Wishlist) = WishEntry.moveWishToWishlist(this,targetWishlist)
}
/*

object Wish {


  val simple = {
    get[Long]("wishid") ~
      get[String]("title") ~
      get[Option[String]]("description") ~
      get[Option[Long]]("reservationid") ~
      get[Long]("recipientid") ~
      get[String]("username")   map {
      case wishid~title~description~reservationid~recipientid~username => {
        val reservation = reservationid.map { reservationId =>
            new Reservation(reservationId) //,new Recipient(recipientid),new Wish(wishid))
        }
        new Wish( wishid, title, description, reservation, new Recipient(recipientid,username))
      }
    }
  }

    def save(wish:Wish) = {
        Logger.debug("Inserting wish: "+wish.title)
        DB.withConnection { implicit connection =>
            val nextId = SQL("SELECT NEXTVAL('wish_seq')").as(scalar[Long].single)
            SQL(
                """
                    insert into wish
                    (wishid,title,description,recipientid)
                    values
                    ({wishid},{title},{description},{recipientid})
                """
            ).on(
                'wishid -> nextId,
                'title -> wish.title,
                'description -> wish.description,
                'recipientid -> wish.recipient.recipientId.get
            ).executeInsert()
            wish.copy(wishId = Some(nextId))
        }
    }


    def findById(wishId:Long) : Option[Wish]= {
        DB.withConnection { implicit connection =>
          SQL(
            """
              SELECT wish.*,recipient.username FROM wish
              INNER JOIN recipient on recipient.recipientid = wish.recipientid
              WHERE wish.wishid = {wishid}
            """
          ).on(
            'wishid -> wishId
          ).as(Wish.simple.singleOpt)
        }
    }


    def delete(wish:Wish) {
        Logger.debug("Deleting wish: "+wish.title)
        DB.withConnection { implicit connection =>
            SQL(
                """
                    delete from wish
                    where wishid = {wishid}
                """
            ).on(
                'wishid -> wish.wishId
            ).execute()
        }
    }

  def update(wish:Wish) = {
    Logger.debug("Updating wish: "+wish.wishId)
    DB.withConnection { implicit connection =>
      SQL(
        """
            update wish
            set title = {title}, description = {description}
            where wishid = {wishid}
        """
      ).on(
        'wishid -> wish.wishId,
        'title -> wish.title,
        'description -> wish.description
      ).executeInsert()
      wish
    }
  }

 def addLinkToWish(wish:Wish,url:String) = {
    DB.withConnection { implicit connection =>
      val nextId = SQL("SELECT NEXTVAL('wishlink_seq')").as(scalar[Long].single)
      SQL(
        """
            insert into wishlink
            (linkid,wishid,url)
            values
            ({linkid},{wishid},{url})
        """
      ).on(
        'linkid -> nextId,
        'wishid -> wish.wishId.get,
        'url -> url
      ).executeInsert()
      nextId
    }
  }

 def deleteLinkFromWish(wish:Wish,linkId:Long)  {
    DB.withConnection { implicit connection =>
      SQL(
        """
            delete from wishlink
            where wishid = {wishid}
            and linkid = {linkid}
        """
      ).on(
        'wishid -> wish.wishId.get,
        'linkid -> linkId
      ).execute()
    }
  }



  def findLink(wish:Wish,linkId:Long) : Option[String]= {
      DB.withConnection { implicit connection =>
        SQL(
          """
            select url from wishlink
            where wishid = {wishid}
            and linkid = {linkid}
          """
        ).on(
          'wishid -> wish.wishId.get,
          'linkid -> linkId
        ).as(scalar[String].singleOpt)
      }
  }


}


*/


case class WishLink(
  linkId : Long,
  wish : Wish,
  url : String
)

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
        wishlist:Wishlist,
        ordinal: Option[Int] = None
) {

   def save(implicit wishEntryRepository: WishEntryRepository) = wishEntryRepository.saveWishEntry(this)

 // def update = WishEntry.update(this)
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


  def removeWishFromWishlist(wish:Wish,wishlist:Wishlist){
    Logger.debug("Deleting wishentry: "+wish.title)
    DB.withConnection { implicit connection =>
      SQL(
        """
            delete from wishentry
            where wishid = {wishid}
            and wishlistid = {wishlistid}
        """
      ).on(
        'wishid -> wish.wishId.get,
        'wishlistid -> wishlist.wishlistId.get
      ).execute()
      SQL(
        """
            delete from wish
            where wishid = {wishid}
        """
      ).on(
        'wishid -> wish.wishId.get
      ).execute()
    }
  }


  def findByIds(wishId:Long,wishlistId:Long) : Option[WishEntry]= {
    DB.withConnection { implicit connection =>
      SQL(
        """
              SELECT we.*,wi.* FROM wishentry we
                LEFT JOIN wish wi ON wi.wishid = we.wishid
              WHERE we.wishid = {wishid}
              AND we.wishlistid = {wishlistid}
        """
      ).on(
        'wishid -> wishId,
        'wishlistid -> wishlistId
      ).as(WishEntry.simple.singleOpt)
    }
  }



  def update(wishentry:WishEntry)= {
    DB.withConnection { implicit connection =>
      SQL(
        """
            update wishentry
            set ordinal = {ordinal}
            where wishid = {wishid}
            and wishlistid = {wishlistid}
        """
      ).on(
        'wishid -> wishentry.wish.wishId.get,
        'ordinal -> wishentry.ordinal,
        'wishlistid -> wishentry.wishlist.wishlistId.get
      ).execute()
      wishentry
    }
  }

  def moveWishToWishlist(wish:Wish,wishlist:Wishlist) = {
    wish.reservation.map{ reservation =>
      if(reservation.isReserver(wishlist.recipient) ){
        Logger.info("Wish unreserved as move target wishlist is reserver and recipient")
        wish.unreserve
    } }
    DB.withConnection { implicit connection =>
      SQL(
        """
            update wishentry
            set wishlistid = {wishlistid}
            where wishid = {wishid}
        """
      ).on(
        'wishid -> wish.wishId.get,
        'wishlistid -> wishlist.wishlistId.get
      ).execute()
    }
    Wish.findById(wish.wishId.get).get
  }

}

*/
