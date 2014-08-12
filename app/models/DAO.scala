package models

import play.api.db.slick.DB
import myUtils.MyPostgresDriver

import scala.slick.lifted.{Query, ProvenShape, ForeignKeyQuery}

/**
* All tables can be configured here for DAO operations
*
*/
class DAO(override val driver: MyPostgresDriver) extends JustRoleComponent with MyProfileComponent with CustomerRoleComponent with RoleComponent with CustomerComponent {
  import driver.simple._
  //object customersobject extends CustomersModel
  object customers extends TableQuery(new Customers(_))
  //val customers = TableQuery(new Customers(_))
  //val myprofiles = TableQuery(new MyProfiles(_))
  //val justroles = TableQuery[JustRoles]
  object myprofiles extends TableQuery(new MyProfiles(_))
  object justroles extends TableQuery(new JustRoles(_))
  //val roles = TableQuery[Roles]
}

object current {
  /**
  * Use the default db settings from conf file for postgres customized db driver
  *
  */
  val dao = new DAO(DB(play.api.Play.current).driver.asInstanceOf[MyPostgresDriver])
}
