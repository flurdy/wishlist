package repositories

import java.sql.Connection

import anorm._
import anorm.SqlParser._
import play.api.db.{DBApi, Database}

trait Repository {

   def dbApi: DBApi

   lazy val db: Database = dbApi.database("default")

   def generateNextId(sequence: String)(implicit connection: Connection) =
      SQL"""SELECT NEXTVAL($sequence)""".as(scalar[Long].single)
}
