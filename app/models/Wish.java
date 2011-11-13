package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class Wish extends Model{

    public String title;

    @ManyToOne
    public Wishlist wishlist;


}
