package models;

import play.db.jpa.Model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Wishlist extends Model {

    public String title;

    @ManyToOne
    public User organiser;

    @ManyToOne
    public User recipient;

     @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL)
    public List<Wish> wishes;

}
