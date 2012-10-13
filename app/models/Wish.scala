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
    ordinal:Option[Int],
    wishlist:Option[Wishlist]
) {
    
    def this(wishId:Option[Long]) = this(wishId,"",None,None,None)

    def save = Wish.save(this)

    def delete = Wish.delete(this)

    def update = Wish.update(this)

    def updateOrdinal = Wish.updateOrdinal(this)

}


object Wish {


  val simple = {
    get[Long]("wishid") ~
      get[String]("title") ~
      get[Option[String]]("description") ~
      get[Option[Int]]("ordinal") ~
      get[Long]("wishlistid") map {
      case wishid~title~description~ordinal~wishlistid => {
        Wishlist.findById(wishlistid) match {
          case Some(wishlist) => Wish( Some(wishid), title, description, ordinal, Some(wishlist))
          case None => { 
            Logger.error("Wish %d wishlist %d not found".format(wishid,wishlistid))
            null
          }
        }
      }
    }
  }


    def save(wish:Wish) = {
        Logger.debug("Inserting wish: "+wish.title)
        DB.withConnection { implicit connection =>
            val nextId = SQL("SELECT NEXTVAL('wish_seq')").as(scalar[Long].single)
            val maxOrdinal = SQL("SELECT MAX(ordinal + 1) from wish").as(scalar[Int].single)
            SQL(
                """
                    insert into wish
                    (wishid,title,description,ordinal,wishlistid) 
                    values 
                    ({wishid},{title},{description},{ordinal},{wishlistid})
                """
            ).on(
                'wishid -> nextId,
                'title -> wish.title,
                'description -> wish.description,
                'ordinal -> wish.ordinal.getOrElse(maxOrdinal),
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

  def update(wish:Wish) = {
    Logger.debug("Updating wish: "+wish.wishId)
    DB.withConnection { implicit connection =>
      SQL(
        """
                    update wish
                    set title = {title}, description = {description}, ordinal = {ordinal}
                    where wishid = {wishid}
        """
      ).on(
        'wishid -> wish.wishId,
        'title -> wish.title,
        'ordinal -> wish.ordinal,
        'description -> wish.description
      ).executeInsert()
      wish
    }
  }

  def updateOrdinal(wish:Wish) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
                    update wish
                    set ordinal = {ordinal}
                    where wishid = {wishid}
        """
      ).on(
        'wishid -> wish.wishId,
        'ordinal -> wish.ordinal
      ).executeInsert()
      wish
    }
  }


}
