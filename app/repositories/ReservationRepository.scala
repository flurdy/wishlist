package repositories

import anorm._
import anorm.JodaParameterMetaData._
import anorm.SqlParser._
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import controllers.WithLogging



@ImplementedBy(classOf[DefaultReservationRepository])
trait ReservationRepository extends Repository with WithLogging {

   def saveReservation(reservation: Reservation) =
      Future {
         (reservation.wish.wishId, reservation.recipient.recipientId) match {
            case (Some(wishId), Some(recipientId)) =>
               logger.info(s"Saving reservation for wish [$wishId] and recipient [$recipientId]")
               db.withConnection{ implicit connection =>
                  val nextId = SQL"""SELECT NEXTVAL('reservation_seq')""".as(scalar[Long].single)
                  SQL"""
         				   insert into reservation
         						(reservationid,recipientid,wishid)
         					values
         						($nextId, $recipientId, $wishId)
                     """
                     .executeInsert()
                  SQL"""
            	         update wish
            	         set reservationid = $nextId
            	         where wishid = $wishId
            	      """
                     .executeUpdate()
         			reservation.copy(reservationId = Some(nextId))
               }
            case _ =>
               throw new IllegalArgumentException("Can not save reservation without id")
         }
      }

}

@Singleton
class DefaultReservationRepository @Inject() (val dbApi: DBApi) extends ReservationRepository
