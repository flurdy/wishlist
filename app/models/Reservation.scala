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
      get[Long]("wishid") map {
      	case reservationid~recipientid~wishid => { 
			Reservation( Some(reservationid), new Recipient(recipientid), new Wish(wishid) )
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


}
