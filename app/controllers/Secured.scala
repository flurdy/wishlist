package controllers

import play.api.mvc._
import models.Recipient


trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def isAuthenticated(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthenticated) { username =>
      Action(request => f(username)(request))
    }
  }

  private def onUnauthenticated(request: RequestHeader) = {
    Results.Redirect(routes.Application.showLoginForm)
  }

  implicit def findCurrentRecipient(implicit session: Session): Option[Recipient] = {
    session.get(Security.username) match {
      case None => None
      case Some(sessionUsername) => Recipient.findByUsername( sessionUsername )
    }
  }

  def withCurrentRecipient(f: Recipient => Request[AnyContent] => Result) = isAuthenticated {
    username => implicit request =>
      Recipient.findByUsername(username).map {
        recipient => f(recipient)(request)
      }.getOrElse(onUnauthenticated(request))
  }


}
