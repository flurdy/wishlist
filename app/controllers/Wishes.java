package controllers;

import models.Wish;
import play.mvc.With;

@CRUD.For(Wish.class)
@With(Secure.class)
@Check("admin")
public class Wishes extends CRUD {

}
