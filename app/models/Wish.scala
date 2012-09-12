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
    wishlist:Option[Wishlist]
) {
    
    def this(wishId:Option[Long]) = this(wishId,"",None,None)

}


object Wish {


  val simple = {
    get[Long]("wishid") ~
      get[String]("title") ~
      get[String]("description") map {
      case wishid~title~description => Wish( Some(wishid), title, Some(description) , None)
    }
  }


    def save(wish:Wish) = {
        Logger.debug("Inserting wish: "+wish.title)
        DB.withConnection { implicit connection =>
            val nextId = SQL("SELECT NEXTVAL('wish_seq')").as(scalar[Long].single)
            SQL(
                """
                    insert into wish
                    (wishid,title,description,wishlistid) 
                    values 
                    ({wishid},{title},{description},{wishlistid})
                """
            ).on(
                'wishid -> nextId,
                'title -> wish.title,
                'description -> wish.description,
                'wishlistid -> wish.wishlist.get.wishlistId
            ).executeInsert()
            wish.copy(wishId = Some(nextId))
        }
    }



    
}
