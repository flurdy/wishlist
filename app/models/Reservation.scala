package models

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import repositories._
import controllers.WithLogging

case class Reservation(
	reservationId: Option[Long],
   reserver: Recipient,
	wish: Wish
) extends WithLogging {

   // def this(reservationId: Long) = this(Some(reservationId),null,null)

   def this(reserver: Recipient, wish: Wish) = this(None, reserver, wish)

   def save(implicit reservationRepository: ReservationRepository): Future[Reservation] =
      reservationRepository.saveReservation(this)

   def cancel(implicit reservationRepository: ReservationRepository, wishRepository: WishRepository) =
      reservationRepository.deleteReservation(this).flatMap { _ =>
         wishRepository.removeReservation(wish)
      }.map( _ => ())

   def isReserver(possibleReserver: Recipient) =
      reserver.isSame(possibleReserver)

   def inflate(implicit recipientRepository: RecipientRepository): Future[Reservation] =
      reserver.inflate.map( r => this.copy( reserver = r ) )

}

/*

object Reservation {


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
