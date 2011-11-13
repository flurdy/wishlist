package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "wishlist_user")
public class User extends Model {

    public String username;
    public String fullname;

}
