package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }

  def search = TODO

  def register = TODO

  def login = TODO
    
   def about = TODO
   
   def contact = TODO
  
}