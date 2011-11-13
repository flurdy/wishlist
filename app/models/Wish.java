package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

@Entity
public class Wish extends Model{

    public String title;

    @Lob
    public String description;

    @ManyToOne
    public Wishlist wishlist;


}
