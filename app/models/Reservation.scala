package models


import play.api.Play.current
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.Logger

case class Reservation(
	reservationId: Option[Long],
	recipient: Recipient,
	wish: Wish
) {

	def this(reservationId: Long) = this(Some(reservationId),null,null)

	def this(recipient: Recipient, wish: Wish) = this(None,recipient,wish)

	def save = Reservation.save(this)
	
}

object Reservation {
	
	val simple = {
   	  get[Long]("reservationid") ~
      get[Long]("recipientid") ~
      get[String]("username") ~
      get[Long]("recipientid") ~
      get[String]("username") ~
      get[Long]("wishid") ~
      get[String]("title") ~
      get[Option[String]]("description")  map {
      	case reservationid~recipientid1~username1~recipientid2~username2~wishid~title~description => {
			Reservation( Some(reservationid), new Recipient(recipientid1,username2), new Wish(wishid,title,description,new Recipient(recipientid2,username2)) )
   		}
   	  }
  	}


  	def create(reservation:Option[Long]) = {
  		reservation map { reservationId =>
  			new Reservation(reservationId)
  		}
  	}



  def save(reservation:Reservation) = {
      Logger.debug("Inserting reservation: "+reservation.wish.wishId.get)
      DB.withConnection { implicit connection =>
         val nextId = SQL("SELECT NEXTVAL('reservation_seq')").as(scalar[Long].single)
      	SQL(
             """
					insert into reservation 
						(reservationid,recipientid,wishid) 
					values 
						({reservationid},{recipientid},{wishid})
             """
         ).on(
				'reservationid -> nextId,
				'recipientid -> reservation.recipient.recipientId.get,
				'wishid -> reservation.wish.wishId.get
			).executeInsert()
	      SQL(
	        """
	            update wish
	            set reservationid = {reservationid}
	            where wishid = {wishid}
	        """
	      ).on(
				'reservationid -> nextId,
				'wishid -> reservation.wish.wishId.get
	      ).executeInsert()
			reservation.copy(reservationId = Some(nextId))
 		}
 	}



	def findByWish(wishId:Long) : Option[Reservation]= {
		DB.withConnection { implicit connection =>
			SQL(
				"""
					SELECT * FROM reservation 
		 			WHERE wishid = {wishid}
				"""
			).on(
				'wishid -> wishId
			).as(Reservation.simple.singleOpt)
		}
	}



	def findByRecipient(recipient:Recipient) : Seq[Reservation]= {
		DB.withConnection { implicit connection =>
			SQL(
				"""
					SELECT res.reservationid,rec1.recipientid,rec1.username,rec2.recipientid,rec2.username,wi.wishid,wi.title,wi.description FROM reservation res
					LEFT JOIN wish wi ON wi.wishid = res.wishid
					LEFT JOIN recipient rec1 ON rec1.recipientid = res.recipientid
					LEFT JOIN recipient rec2 ON rec2.recipientid = wi.recipientid
		 			WHERE res.recipientid = {recipientid}
		 			ORDER BY res.wishid desc
				"""
			).on(
				'recipientid -> recipient.recipientId.get
			).as(Reservation.simple *)
		}
	}


}
