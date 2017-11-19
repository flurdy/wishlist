package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.mvc.Results.{NotFound, Unauthorized}
import scala.concurrent.{ExecutionContext, Future}
import models._
import repositories.RecipientLookup

class UsernameRequest[A](val username: Option[String], request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class UsernameAction @Inject()(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
      extends ActionBuilder[UsernameRequest, AnyContent] with ActionTransformer[Request, UsernameRequest] {
   def transform[A](request: Request[A]) = Future.successful {
      new UsernameRequest(request.session.get("username"), request)
   }
}

class MaybeCurrentRecipientRequest[A](val currentRecipient: Option[Recipient], request: Request[A]) extends WrappedRequest[A](request) {
  def username = request.session.get("username")
}

class MaybeCurrentRecipientAction @Inject()(val parser: BodyParsers.Default)
      (implicit val executionContext: ExecutionContext, recipientLookup: RecipientLookup)
      extends ActionBuilder[MaybeCurrentRecipientRequest, AnyContent] with ActionTransformer[Request, MaybeCurrentRecipientRequest] {
   def transform[A](request: Request[A]) = {
      request.session.get("username").fold[Future[Option[Recipient]]]{
        Future.successful(None) 
      }{ username => 
          recipientLookup.findRecipient(username)
    }.map { currentRecipient =>
          new MaybeCurrentRecipientRequest(currentRecipient, request)
      } 
   }
}


trait Secured {

   def recipientLookup: RecipientLookup

   implicit def requestToCurrentRecipient(implicit request: MaybeCurrentRecipientRequest[_]): Option[Recipient] = request.currentRecipient

   // def PermissionCheckAction(implicit ec: ExecutionContext) = new ActionFilter[ItemRequest] {
   //    def executionContext = ec
   //    def filter[A](input: ItemRequest[A]) = Future.successful {
   //       if (!input.item.accessibleByUser(input.username))
   //          Some(Forbidden)
   //       else
   //          None
   //    }
   // }

   // def CurrentRecipientAction(implicit ec: ExecutionContext, recipientLookup: RecipientLookup) = new ActionFilter[UsernameRequest] {
   //    def filter[A](input: UsernameRequest[A]) = {
   //       input.username.fold{
   //          implicit val flash = input.flash
   //          implicit val currentRecipient = None
   //          Future.successful( NotFound(views.html.error.notfound()) )
   //       }{ username =>
   //          recipientLookup.findRecipient(username).map( recipient =>
   //             new MaybeCurrentRecipientRequest(recipient, input) )
   //       }
   //    }
   // }

   //
   // def CurrentRecipientAction = new ActionRefiner[UsernameRequest, MaybeCurrentRecipientRequest] {
   //    def refine[A](input: UsernameRequest[A]) =
   //       input.username match {
   //          case Some(username) =>
   //             recipientLookup.findRecipient(username).map( recipient =>
   //                Right( new MaybeCurrentRecipientRequest(recipient, input) ) )
   //          case None =>
   //             implicit val flash = input.flash
   //             implicit val currentRecipient = None
   //             Future.successful( Left(NotFound(views.html.error.notfound())) )
   //       }
   // }
  //
  implicit def findCurrentRecipient(implicit session: Session, ec: ExecutionContext): Future[Option[Recipient]] = {
    session.get("username") match {
      case Some(sessionUsername) => recipientLookup.findRecipient(sessionUsername)
      case None => Future.successful(None)
    }
  }

}
