package models

import play.api.Play.current
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.Logger

case class Wishlist(
    wishlistId:Option[Long],
    title:String,
    description:Option[String],
    recipient:Recipient,
    organisers:Seq[Recipient] = Seq.empty
) {

    def this(wishlistId:Long) = this(Some(wishlistId),"",None,null)

    def save = Wishlist.save(this)

    def delete = Wishlist.delete(this)

    def update = Wishlist.update(this)

    def removeWish(wish:Wish) = WishEntry.removeWishFromWishlist(wish,this)

    def findWishes = Wishlist.findWishesForWishlist(this)

    def findOrganisers = Wishlist.findOrganisers(this)

    def addOrganiser(organiser:Recipient) = Wishlist.addOrganiserToWishlist(organiser,this)

    def removeOrganiser(organiser:Recipient) = Wishlist.removeOrganiserFromWishlist(organiser,this)

    def isOrganiser(organiser:Recipient) = Wishlist.isOrganiserOfWishlist(organiser,this)
}

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


    def save(wishlist:Wishlist) = {
        Logger.debug("Inserting wishlist: "+wishlist.title)
        DB.withConnection { implicit connection =>
            val nextId = SQL("SELECT NEXTVAL('wishlist_seq')").as(scalar[Long].single)
            SQL(
                """
                    insert into wishlist
                    (wishlistid,title,description,recipientid) 
                    values 
                    ({wishlistid},{title},{description},{recipientid})
                """
            ).on(
                'wishlistid -> nextId,
                'title -> wishlist.title,
                'description -> wishlist.description,
                'recipientid -> wishlist.recipient.recipientId
            ).executeInsert()
            wishlist.copy(wishlistId = Some(nextId))
        }
    }


    def findById(wishlistId:Long) : Option[Wishlist]= {
        DB.withConnection { implicit connection =>
          SQL(
            """
              SELECT * FROM wishlist
                WHERE wishlistid = {wishlistid}
            """
          ).on(
            'wishlistid -> wishlistId
          ).as(Wishlist.simple.singleOpt)
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


    def update(wishlist:Wishlist) = {
        Logger.debug("Updating wishlist: "+wishlist.title)
        DB.withConnection { implicit connection =>
            val nextId = SQL("SELECT NEXTVAL('wishlist_seq')").as(scalar[Long].single)
            SQL(
                """
                    update wishlist
                    set title = {title}, description = {description}
                    where wishlistid = {wishlistid}
                """
            ).on(
                'wishlistid -> wishlist.wishlistId,
                'title -> wishlist.title,
                'description -> wishlist.description
            ).executeInsert()
            wishlist
        }
    }

    def findAll : Seq[Wishlist] = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                  SELECT * FROM wishlist
                  ORDER BY title DESC
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
                  ORDER BY title DESC
                """
            ).on(
                'term -> searchLikeTerm
            ).as(Wishlist.simple *)
        }
    }


    def findWishesForWishlist(wishlist:Wishlist) : Seq[Wish] = {
        DB.withConnection { implicit connection =>
            SQL(
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
        }
    }



//    def findWishlistsByUsername(username:String) : Seq[Wishlist] = {
//        val recipient = Recipient.findByUsername(username).get
//        DB.withConnection { implicit connection =>
//            SQL(
//                """
//                  SELECT * FROM wishlist
//                  where recipientid = {recipientid}
//                  ORDER BY title DESC
//                """
//            ).on(
//                'recipientid -> recipient.recipientId.get
//            ).as(Wishlist.simple *)
//        }
//    }


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
                  SELECT * FROM wishlist wi
                  INNER JOIN wishlistorganiser wo on wo.wishlistid = wi.wishlistid
                  WHERE wo.recipientid = {recipientid}
                  ORDER BY title DESC
                """
            ).on(
                'recipientid -> organiser.recipientId.get
            ).as(Wishlist.simple *)
        }
    }



    def findOrganisers(wishlist:Wishlist) : Seq[Recipient] = {
        DB.withConnection { implicit connection =>
            SQL(
              """
                  SELECT rec.*
                  FROM recipient rec
                  INNER JOIN wishlistorganiser wo on wo.recipientid = rec.recipientid
                  where wo.wishlistid = {wishlistid}
                  ORDER BY rec.username
              """
            ).on(
                'wishlistid -> wishlist.wishlistId
            ).as(Recipient.simple *)
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

  def isOrganiserOfWishlist(organiser:Recipient,wishlist:Wishlist) = {   
    DB.withConnection { implicit connection =>
      SQL(
        """
              SELECT count(*) = 1 FROM wishlistorganiser
              WHERE recipientid = {recipientid}
              AND wishlistid = {wishlistid}
        """
      ).on(
        'wishlistid -> wishlist.wishlistId.get,
        'recipientid -> organiser.recipientId.get
      ).as(scalar[Boolean].single)
    }
  }

}

