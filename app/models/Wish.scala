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

    def save = Wish.save(this)


    def delete = Wish.delete(this)

}


object Wish {


  val simple = {
    get[Long]("wishid") ~
      get[String]("title") ~
      get[Option[String]]("description") map {
      case wishid~title~description => Wish( Some(wishid), title, description , None)
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

    
}
