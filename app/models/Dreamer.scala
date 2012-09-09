package models


import play.api.Play.current
import org.mindrot.jbcrypt.BCrypt
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.Logger

case class Dreamer (
    dreamerId: Option[Long],
    username: String,
    fullname: Option[String],
    email: String,
    password: Option[String]
){

    def save = Dreamer.save(this)

}

object Dreamer {

  val simple = {
    get[Long]("dreamerid") ~
      get[String]("username") ~
      get[Option[String]]("fullname") ~
      get[String]("email")  map {
      case dreamerid~username~fullname~email=> Dreamer( Some(dreamerid), username, fullname, email, None)
    }
  }

  val authenticationMapper = {
    get[Long]("dreamerid") ~
      get[String]("username") ~
      get[Option[String]]("password")  map {
      case dreamerid~username~password => Dreamer( Some(dreamerid), username, null, null, password )
    }
  }

  def encrypt(passwordOption: Option[String]) = {
    passwordOption.map { password =>
      Some(BCrypt.hashpw(password,BCrypt.gensalt()))
    }.getOrElse(None)
  }

    def save(dreamer:Dreamer) = {
        Logger.debug("Inserting dreamer: "+dreamer.username)
        findByUsername(dreamer.username) match {
            case None => {
                DB.withConnection { implicit connection =>
                    val nextDreamerId = SQL("SELECT NEXTVAL('dreamer_seq')").as(scalar[Long].single)
                    SQL(
                        """
                            insert into dreamer 
                            (dreamerid,username,fullname,email,password) 
                            values 
                            ({dreamerid},{username},{fullname},{email},{password})
                        """
                    ).on(
                        'dreamerid -> nextDreamerId,
                        'username -> dreamer.username,
                        'fullname -> dreamer.fullname,
                        'email -> dreamer.email,
                        'password -> encrypt(dreamer.password)
                    ).executeInsert()
                    dreamer.copy(dreamerId = Some(nextDreamerId),password=None)
                }
            }
            case Some(existingDreamer) => throw new IllegalArgumentException("Username already exists")
        }
    }

    def findByUsername(username:String) : Option[Dreamer]= {
        DB.withConnection { implicit connection =>
          SQL(
            """
              SELECT * FROM dreamer
                WHERE username = {username}
            """
          ).on(
            'username -> username.trim
          ).as(Dreamer.simple.singleOpt)
        }
    }

    def findById(dreamerId:Long) : Option[Dreamer]= {
        DB.withConnection { implicit connection =>
          SQL(
            """
              SELECT * FROM dreamer
                WHERE dreamerid = {dreamerid}
            """
          ).on(
            'dreamerid -> dreamerId
          ).as(Dreamer.simple.singleOpt)
        }
    }


    def findAuthenticationDetailsByUsername(username:String) : Option[Dreamer]= {
        DB.withConnection { implicit connection =>
          SQL(
            """
              SELECT * FROM dreamer
                WHERE username = {username}
            """
          ).on(
            'username -> username.trim
          ).as(Dreamer.authenticationMapper.singleOpt)
        }
    }

    def authenticate(username: String, password: String) : Option[Dreamer]  = {
        findAuthenticationDetailsByUsername(username) match { 
            case Some(dreamer) =>
                if(BCrypt.checkpw(password,dreamer.password.get)){
                    findByUsername(username)
                } else {
                    None
                }
            case None => None
        }
    }


}

