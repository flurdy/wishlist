package models


// import play.api.Play.current
// import play.api.db.DB
// import anorm._
// import anorm.SqlParser._
// import play.Logger
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import repositories._

case class Reservation(
	reservationId: Option[Long],
   reserver: Recipient,
	wish: Wish
) {

   // def this(reservationId: Long) = this(Some(reservationId),null,null)

   def this(recipient: Recipient, wish: Wish) = this(None,recipient,wish)

   def save(implicit reservationRepository: ReservationRepository): Future[Reservation] =
      reservationRepository.saveReservation(this)

   def cancel(implicit reservationRepository: ReservationRepository, wishRepository: WishRepository) =
      reservationRepository.deleteReservation(this).flatMap { _ =>
         wishRepository.removeReservation(wish)
      }.map( _ => ())

   def isReserver(possibleReserver: Recipient) = reserver.isSame(possibleReserver)

}

/*

object Reservation {


  	def create(reservation:Option[Long]) = {
  		reservation map { reservationId =>
  			new Reservation(reservationId)
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


  def findReserver(reservation:Reservation) : Option[Recipient]= {
    DB.withConnection { implicit connection =>
      SQL(
        """
					SELECT rec.* FROM reservation res
          INNER JOIN recipient rec on res.recipientid = rec.recipientid
		 			WHERE res.reservationid = {reservationid}
        """
      ).on(
        'reservationid -> reservation.reservationId
      ).as(Recipient.simple.singleOpt)
    }
  }


}

*/
