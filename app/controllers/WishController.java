package controllers;

import models.User;
import models.Wish;
import models.Wishlist;
import play.mvc.Controller;

import java.util.List;

public class WishController extends Controller {


    public static void listWishlists(String username){
        final User recipient = User.find("byUsername",username).first();
        final List<Wishlist> wishlists= Wishlist.find("byRecipient",recipient).fetch();
        render(recipient,wishlists);
    }


    public static void showWishlist(String username, Long listId){
        final User recipient = User.find("byUsername",username).first();
        final Wishlist wishlist= Wishlist.findById(listId);
        render(recipient,wishlist);
    }



    public static void showWish(String username, Long listId, Long wishId){
        final User recipient = User.find("byUsername",username).first();
        final Wishlist wishlist= Wishlist.findById(listId);
        final Wish wish= Wish.findById(wishId);
        render(recipient,wishlist,wish);
    }


}
