package controllers;

import play.mvc.With;

@With(Secure.class)
@Check("superUser")
public class Users extends CRUD {
}
