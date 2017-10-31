package controllers

import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.mvc.Results.{NotFound, Unauthorized}
import scala.concurrent.Future
import models._
import repositories.RecipientLookup


class UsernameRequest[A](val username: Option[String], request: Request[A]) extends WrappedRequest[A](request)

class MaybeCurrentRecipientRequest[A](val currentRecipient: Option[Recipient], request: Request[A]) extends WrappedRequest[A](request){
   lazy val username = request.session.get(Security.username)
}

object UsernameAction extends
    ActionBuilder[UsernameRequest] with ActionTransformer[Request, UsernameRequest] {
  def transform[A](request: Request[A]) = Future.successful {
    new UsernameRequest(request.session.get(Security.username), request)
  }
}

object MaybeCurrentRecipientAction extends
    ActionBuilder[MaybeCurrentRecipientRequest] with ActionTransformer[Request, MaybeCurrentRecipientRequest] {
  def transform[A](request: Request[A]) = Future.successful {
    new MaybeCurrentRecipientRequest(request.session.get(Security.username).map(new Recipient(_)), request)
  }
}

object IsAuthenticatedAction extends ActionFilter[UsernameRequest] {
  def filter[A](input: UsernameRequest[A]) = Future.successful {
     if(input.username.isEmpty) Some(Unauthorized)
     else None
  }
}


trait Secured {

   def recipientLookup: RecipientLookup

   implicit def requestToCurrentRecipient(implicit request: MaybeCurrentRecipientRequest[_]): Option[Recipient] = request.currentRecipient

   def CurrentRecipientAction = new ActionRefiner[UsernameRequest, MaybeCurrentRecipientRequest] {
      def refine[A](input: UsernameRequest[A]) =
         input.username match {
            case Some(username) =>
               recipientLookup.findRecipient(username).map( recipient =>
                  Right( new MaybeCurrentRecipientRequest(recipient, input) ) )
            case None =>
               implicit val flash = input.flash
               implicit val currentRecipient = None
               Future.successful( Left(NotFound(views.html.error.notfound())) )
         }
   }

  implicit def findCurrentRecipient(implicit session: Session): Future[Option[Recipient]] = {
    session.get(Security.username) match {
      case Some(sessionUsername) => recipientLookup.findRecipient(sessionUsername)
      case None => Future.successful(None)
    }
  }

}
