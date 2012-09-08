package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }

  def search = TODO

  def register = TODO

  def redirectToRegisterForm = Action {
  	Ok(views.html.application.register())
  }

  def showRegisterForm = Action {
  	Ok(views.html.application.register())
  }

  def login = TODO
    
  def showLoginForm = Action {
  	Ok(views.html.application.login())
  }

   def about = TODO
   
   def contact = TODO
  
}