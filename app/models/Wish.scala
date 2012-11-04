package models

import play.api.Play.current
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.Logger

case class Wish(
    wishId:Option[Long],
    title:String,
    description:Option[String],
    wishEntries:Set[WishEntry] = Set.empty,
    reservation:Option[Reservation] = None,
    recipient:Recipient
) {

    def this(wishId:Long,
        title:String,
        description:Option[String],
        reservation:Option[Reservation],recipient:Recipient) =
          this(Some(wishId), title, description, Set.empty, reservation, recipient)

    def this(wishId:Long) = this(Some(wishId),"",None, Set.empty, None , null)

    def this(wishId:Long,title:String,description:Option[String],recipient:Recipient) = this(Some(wishId),title,description, Set.empty, None, recipient:Recipient)

    def this(title:String,description:Option[String],recipient:Recipient) = this(None,title,description, Set.empty, None, recipient:Recipient)

    def save = Wish.save(this)

    def delete = Wish.delete(this)

    def update = Wish.update(this)

    def reserve(recipient:Recipient) = new Reservation(recipient,this).save

    def unreserve = reservation map ( realReservation => realReservation.copy(wish=this).cancel )

    def addToWishlist(wishlist:Wishlist) = WishEntry.addWishToWishlist(this,wishlist)

}


object Wish {


  val simple = {
    get[Long]("wishid") ~
      get[String]("title") ~
      get[Option[String]]("description") ~
      get[Option[Long]]("reservationid") ~
      get[Long]("recipientid")   map {
      case wishid~title~description~reservationid~recipientid => {
        val reservation = reservationid.map { reservationId =>
            new Reservation(reservationId) //,new Recipient(recipientid),new Wish(wishid))
        }
        new Wish( wishid, title, description, reservation, new Recipient(recipientid))
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
              SELECT * FROM wish
              WHERE wishid = {wishid} 
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


}




case class WishEntry(
        wish:Wish,
        wishlist:Wishlist,
        ordinal: Option[Int]
) {

 def update = WishEntry.update(this)

}


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

  def addWishToWishlist(wish:Wish,wishlist:Wishlist) {
    DB.withConnection { implicit connection =>
      val maxOrdinal = SQL(
        """
          SELECT COALESCE(MAX(ordinal),0) + 1 from wishentry
          where wishlistid = {wishlistid}
        """
        ).on(
            'wishlistid -> wishlist.wishlistId.get
        ).as(scalar[Int].single)
      SQL(
        """
            insert into wishentry
            (wishid,wishlistid,ordinal)
            values
            ({wishid},{wishlistid},{ordinal})
        """
      ).on(
        'wishid -> wish.wishId.get,
        'wishlistid -> wishlist.wishlistId.get,
        'ordinal -> maxOrdinal
      ).executeInsert()
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
      ).executeInsert()
      wishentry
    }
  }



}