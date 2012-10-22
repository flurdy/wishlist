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
	
}

object Reservation {
	
	val simple = {
   	get[Long]("reservationid") ~
    	get[Long]("recipientid") ~
    	get[Long]("wishid") map {
      	case reservationid~recipientid~wishid => { 
      		Wish.findById(wishid) flatMap { wish =>
      			Recipient.findById(recipientid) flatMap { recipient =>
		      		Reservation( Some(reservationid), recipient, wish )
      			}
      		}
			} 
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
			reservation.copy(reservationId = Some(nextId))
 		}
 	}



	def findById(reservationId:Long) : Option[Reservation]= {
		DB.withConnection { implicit connection =>
			SQL(
				"""
					SELECT * FROM reservation
		 			WHERE reservationid = {reservationid}
				"""
			).on(
				'reservationid -> reservationId
			).as(Reservation.simple.singleOpt)
		}
	}


}
