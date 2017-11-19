package repositories

import anorm._
import anorm.SqlParser._
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.db._
import scala.concurrent.{ExecutionContext, Future}
import models._
import controllers.WithLogging

trait ReservationMapper {
   val MapToShallowReservation = {
      get[Long]("reservationid") ~
      get[Long]("recipientid") ~
      get[String]("username") ~
      get[Long]("recipientid") ~
      get[String]("username") ~
      get[Long]("wishid") ~
      get[String]("title") ~
      get[Option[String]]("description")  map {
         case reservationid~reserverId~username1~recipientId~username2~wishid~title~description => {
            Reservation( Some(reservationid), new Recipient(reserverId),
               new Wish(wishid, title, description, new Recipient(recipientId)) )
         }
      }
   }
}

@ImplementedBy(classOf[DefaultReservationRepository])
trait ReservationRepository extends Repository with WithLogging
with ReservationMapper {

   def saveReservation(reservation: Reservation)(implicit ec: ExecutionContext) =
      Future {
         (reservation.wish.wishId, reservation.reserver.recipientId) match {
            case (Some(wishId), Some(recipientId)) =>
               logger.debug(s"Saving reservation for wish [$wishId] and recipient [$recipientId]")
               db.withConnection{ implicit connection =>
                  val nextId = generateNextId("reservation_seq")
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

   def findReservationsByReserver(reserver: Recipient)(implicit ec: ExecutionContext) =
      Future {
         reserver.recipientId.fold{
            throw new IllegalStateException("No recipient id")
         } { reserverId =>
            db.withConnection { implicit connection =>
               SQL"""
                     SELECT res.reservationid,rec1.recipientid,rec1.username,rec2.recipientid,rec2.username,wi.wishid,wi.title,wi.description
                     FROM reservation res
                     LEFT JOIN wish wi ON wi.wishid = res.wishid
                     LEFT JOIN recipient rec1 ON rec1.recipientid = res.recipientid
                     LEFT JOIN recipient rec2 ON rec2.recipientid = wi.recipientid
                     WHERE res.recipientid = $reserverId
                     ORDER BY res.wishid desc
				      """
                  .as(MapToShallowReservation *)
            }
         }
      }

   def inflateReservationsReserver(shallowReservations: List[Reservation])
   (implicit ec: ExecutionContext, recipientRepository: RecipientRepository): Future[List[Reservation]] = {
      val thickerReservations =
         shallowReservations.flatMap { reservation =>
            reservation.reserver.recipientId map { recipientId =>
               recipientRepository.findRecipientById(recipientId) map {
                  _.map( r => reservation.copy(reserver = r))
               }
            }
         }
      Future.sequence(thickerReservations).map( _.flatten )
   }

   def inflateReservationsWishRecipient(shallowReservations: List[Reservation])
   (implicit ec: ExecutionContext, recipientRepository: RecipientRepository): Future[List[Reservation]] = {
      val thickerReservations =
         shallowReservations.flatMap { reservation =>
            reservation.wish.recipient.recipientId map { recipientId =>
               recipientRepository.findRecipientById(recipientId) map { recipient =>
                  recipient.map( r => reservation.copy(wish = reservation.wish.copy( recipient = r)))
               }
            }
         }
      Future.sequence(thickerReservations).map( _.flatten )
   }


   def deleteReservation(reservation:Reservation)(implicit ec: ExecutionContext) =
      Future {
         reservation.reservationId.fold {
            throw new IllegalStateException("No recipient id")
         } { reservationId =>
            db.withConnection { implicit connection =>
               logger.debug("Cancelling reservation: " + reservation.wish.wishId)
               SQL"""
                     delete  from reservation
                     where reservationid = $reservationId
                  """
                  .executeUpdate()
            }
         }
      }

}

@Singleton
class DefaultReservationRepository @Inject() (val dbApi: DBApi) extends ReservationRepository
