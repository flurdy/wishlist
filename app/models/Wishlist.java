package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.List;

@Entity
public class Wishlist extends Model {

    public String title;

    @ManyToOne
    public User recipient;


}
