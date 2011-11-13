package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        List<User> users = User.findAll();
        List<Wishlist> wishlists= Wishlist.findAll();
        render(users,wishlists);
    }

    public static void about() {
        render();
    }

    public static void contact() {
        render();
    }

}