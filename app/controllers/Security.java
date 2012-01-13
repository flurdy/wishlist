package controllers;

import models.User;

public class Security extends Secure.Security {

    static boolean authenticate(String username,String password){
        return User.connect(username,password) != null;
    }

    static boolean check(String profile){
        if("admin".equalsIgnoreCase(profile)){
            return User.find("byUsername",connected()).<User>first().isAdmin;
        } else if("superUser".equalsIgnoreCase(profile)){
            return User.find("byUsername",connected()).<User>first().isSuperUser;
        }
        return false;
    }
}
