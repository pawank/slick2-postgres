package models

import play.api.db.slick.DB
import myUtils.MyPostgresDriver

import scala.slick.lifted.{Query, ProvenShape, ForeignKeyQuery}
/**
* All tables can be configured here for DAO operations
*
*/
class DAO(override val driver: MyPostgresDriver) extends MyProfileComponent with CustomerRoleComponent with RoleComponent with CustomerComponent {
  import driver.simple._
  object customers extends TableQuery(new Customers(_))
  object myprofiles extends TableQuery(new MyProfiles(_))
  //object roles extends TableQuery(new Roles(_))
  object baseroles extends TableQuery(tag => new BaseRoles(tag))
  //val roles = TableQuery[Roles]
}

object current {
  /**
  * Use the default db settings from conf file for postgres customized db driver
  *
  */
  val dao = new DAO(DB(play.api.Play.current).driver.asInstanceOf[MyPostgresDriver])
}
