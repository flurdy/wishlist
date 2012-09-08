package models

/*
import play.data.validation.Email;
import play.data.validation.MinSize;
import play.data.validation.Password;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "wishlist_user")
public class User extends Model {

    @Required
    @MinSize(3)
    public String username;

    @Required
    public String fullname;

    @Email
    public String email;

    @MinSize(4)
    @Password
    @Required
    public String password;

    public boolean admin;

    public boolean superUser;

    public static User connect(String username, String password){
        return find("byUsernameAndPassword",username,password).first();
    }

    @Override
    public String toString() {
        return username;
    }
}
*/
