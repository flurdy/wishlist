package models

import play.api.Play.current
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.Logger

case class Wishlist(
    wishlistId:Option[Long],
    title:String,
    recipient:Dreamer,
    organiser:Dreamer
) {



}

object Wishlist {


  val simple = {
    get[Long]("wishlistid") ~
      get[String]("title") ~
      get[Long]("recipientid") ~
      get[Long]("organiserid")  map {
      case wishlistid~title~recipientid~organiserid=> Wishlist( Some(wishlistid), title, Dreamer.findById(recipientid).get, Dreamer.findById(organiserid).get)
    }
  }


    def save(wishlist:Wishlist) = {
        Logger.debug("Inserting wishlist: "+wishlist.title)
        DB.withConnection { implicit connection =>
            val nextId = SQL("SELECT NEXTVAL('wishlist_seq')").as(scalar[Long].single)
            SQL(
                """
                    insert into wishlist
                    (wishlistid,title,recipientid,organiserid) 
                    values 
                    ({wishlistid},{title},{recipientid},{organiserid})
                """
            ).on(
                'wishlistid -> nextId,
                'title -> wishlist.title,
                'recipientid -> wishlist.recipient.dreamerId,
                'organiserid -> wishlist.organiser.dreamerId
            ).executeInsert()
            wishlist.copy(wishlistId = Some(nextId))
        }
    }


}

