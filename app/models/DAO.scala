package models

import play.api.db.slick.DB
import myUtils.MyPostgresDriver

class DAO(override val driver: MyPostgresDriver) extends CustomerComponent {
  import driver.simple._

  val customers = TableQuery(new Customers(_))
}

object current {
  val dao = new DAO(DB(play.api.Play.current).driver.asInstanceOf[MyPostgresDriver])
}
