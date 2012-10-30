package models


import play.api.Play.current
import play.Logger
import play.api.db.DB
import anorm._
import anorm.SqlParser._


case class WishEntry(
        wish:Wish,
        wishlist:Wishlist,
        ordinal: Int
) {

 def update = WishEntry.updateOrdinal(this)

}


object WishEntry {


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
              SELECT * FROM wishentry
              WHERE wishid = {wishid}
              and wishlistid = {wishlistid}
        """
      ).on(
        'wishid -> wishId,
        'wishlistid -> wishlistId
      ).as(WishEntry.simple.singleOpt)
    }
  }



  def updateOrdinal(wishentry:WishEntry)= {
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