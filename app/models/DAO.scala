package models

import play.api.db.slick.DB
import myUtils.MyPostgresDriver

/**
* All tables can be configured here for DAO operations
*
*/
class DAO(override val driver: MyPostgresDriver) extends CustomerComponent with MyProfileComponent with RoleComponent with CustomerRoleComponent with JustRoleComponent {
  import driver.simple._

  val customers = TableQuery(new Customers(_))
  val myprofiles = TableQuery(new MyProfiles(_))
  val justroles = TableQuery[JustRoles]
  //val roles = TableQuery[Roles]
}

object current {
  /**
  * Use the default db settings from conf file for postgres customized db driver
  *
  */
  val dao = new DAO(DB(play.api.Play.current).driver.asInstanceOf[MyPostgresDriver])
}
