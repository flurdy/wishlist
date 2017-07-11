package repositories

import play.api.db.{Database, DBApi}

trait Repository {

   def dbApi: DBApi

   lazy val db: Database = dbApi.database("default")

}
