package models;

import play.data.validation.MaxSize;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

@Entity
public class Wish extends Model{

    public String title;

    @Lob
    @MaxSize(1000)
    public String description;

    @ManyToOne
    public Wishlist wishlist;

    @Override
    public String toString() {
        return title;
    }
}
