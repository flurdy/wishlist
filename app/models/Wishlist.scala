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
    recipient:Dreamer,
    organiser:Dreamer
) {


    def save = Wishlist.save(this)


}

object Wishlist {


  val simple = {
    get[Long]("wishlistid") ~
      get[String]("title") ~
      get[Option[String]]("description") ~
      get[Long]("recipientid") ~
      get[Long]("organiserid")  map {
      case wishlistid~title~description~recipientid~organiserid=> Wishlist( Some(wishlistid), title, description, Dreamer.findById(recipientid).get, Dreamer.findById(organiserid).get)
    }
  }


    def save(wishlist:Wishlist) = {
        Logger.debug("Inserting wishlist: "+wishlist.title)
        DB.withConnection { implicit connection =>
            val nextId = SQL("SELECT NEXTVAL('wishlist_seq')").as(scalar[Long].single)
            SQL(
                """
                    insert into wishlist
                    (wishlistid,title,description,recipientid,organiserid) 
                    values 
                    ({wishlistid},{title},{description},{recipientid},{organiserid})
                """
            ).on(
                'wishlistid -> nextId,
                'title -> wishlist.title,
                'description -> wishlist.description,
                'recipientid -> wishlist.recipient.dreamerId,
                'organiserid -> wishlist.organiser.dreamerId
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

}

