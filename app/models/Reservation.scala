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

	def this(recipient: Recipient,wish: Wish) = this(None,recipient,wish)

	def save = Reservation.save(this)
	
}

object Reservation {
	
	val simple = {
   	  get[Long]("res.reservationid") ~
      get[Long]("res.recipientid") ~
      get[Long]("res.wishid") map {
      	case reservationid~recipientid~wishid => { 
			Reservation( Some(reservationid), Recipient.findById(recipientid).get, Wish.findById(wishid).get )
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
