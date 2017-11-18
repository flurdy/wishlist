package models

import scala.concurrent.{ExecutionContext, Future}
import repositories._
import controllers.WithLogging

case class Reservation(
	reservationId: Option[Long],
   reserver: Recipient,
	wish: Wish
) extends WithLogging {

   // def this(reservationId: Long) = this(Some(reservationId),null,null)

   def this(reserver: Recipient, wish: Wish) = this(None, reserver, wish)

   def save(implicit reservationRepository: ReservationRepository, ec: ExecutionContext): Future[Reservation] =
      reservationRepository.saveReservation(this)

   def cancel(implicit reservationRepository: ReservationRepository, wishRepository: WishRepository, ec: ExecutionContext) =
      reservationRepository.deleteReservation(this).flatMap { _ =>
         wishRepository.removeReservation(wish)
      }.map( _ => ())

   def isReserver(possibleReserver: Recipient) =
      reserver.isSame(possibleReserver)

   def inflate(implicit recipientRepository: RecipientRepository, ec: ExecutionContext): Future[Reservation] =
      reserver.inflate.map( r => this.copy( reserver = r ) )

}
