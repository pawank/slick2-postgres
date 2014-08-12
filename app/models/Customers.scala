package models

import com.vividsolutions.jts.geom.Point
import org.joda.time.{LocalDateTime,DateTime}
import play.api.libs.json.JsValue
import myUtils.{WithMyDriver}

object AccountStatuses extends Enumeration {
  type AccountStatus = Value
  val NOT_REGISTERED, REGISTERED = Value
}
import AccountStatuses._

sealed trait Active
case object Enabled extends Active
case object Disabled extends Active

sealed trait BaseRole {
  def id:Option[Int]
  def code:String
  def name:String
}
case object SuperAdmin extends BaseRole {
  val id = Some(1)
  val code = "Super"
  val name = "SuperAdmin"
  val isAdmin = true
  val isSuperAdmin = true
}
trait WithAdminRole {
  final val isAdmin:Boolean = true
}

case class Role(val id:Option[Int], val code:String, name:String, isAdmin:Boolean = false) extends BaseRole
case class Admin(val id:Option[Int], val code:String, name:String) extends BaseRole with WithAdminRole


case class Address(line1:String, line2:Option[String], city:String)
case class Customer(
  id:Option[Int],
  name:String,
  email:String,
  address:Address,
  status:AccountStatus,
  active:Boolean,
  dob: LocalDateTime,
  interests: List[String],
  others: Option[JsValue],
  enabled:Active,
  createdOn:DateTime
)

trait CustomerComponent extends WithMyDriver{
  import driver.simple._
object ActiveImplicits {
  implicit val activeTypeMapper = MappedColumnType.base[Active, Boolean](
      {
        b => b match {
        case Enabled => true
        case Disabled => false
        }
      }, 
      { 
        i => i match {
        case true => Enabled
        case false => Disabled
        }
      }
      )

}
  import ActiveImplicits._

  class Customers(tag: Tag) extends Table[Customer](tag, "users") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def email = column[String]("email")
    def line1 = column[String]("line1")
    def line2 = column[Option[String]]("line2")
    def city = column[String]("city")
    def address = (line1,line2,city) <> (Address.tupled,Address.unapply)
    /**
    * Create the desired ENUM type in db using {{{create type account_status as enum ('NOT_REGISTERED','REGISTERED');}}}
    */
    def status = column[AccountStatus]("status",O.Default(NOT_REGISTERED))
    def active = column[Boolean]("active")
    def dob = column[LocalDateTime]("dob")
    def interests = column[List[String]]("interests")
    def others = column[Option[JsValue]]("others")
    def enabled = column[Active]("enabled")
    def createdOn = column[DateTime]("created_on")

    def * = (id.?, name, email, address, status, active, dob, interests, others, enabled, createdOn) <> (Customer.tupled, Customer.unapply)
  }
}

trait JustRoleComponent extends WithMyDriver{
  import driver.simple._
  import scala.slick.lifted.{ProvenShape, ForeignKeyQuery}

  class JustRoles(tag: Tag) extends Table[Role](tag, "just_roles") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    def code = column[String]("code")
    def name = column[String]("name")
    def isAdmin = column[Boolean]("is_admin", O.Default(false))
    def isSuperAdmin = column[Option[Boolean]]("is_admin")
    def modelType = column[String]("model_type")

    def * :ProvenShape[Role] = (id.?,code,name,isAdmin) <> (Role.tupled,Role.unapply)
  }
}

trait CustomerRoleComponent extends WithMyDriver{
  import driver.simple._
  import models.current.dao._
  import scala.slick.lifted.{ProvenShape, ForeignKeyQuery}

  type CustomerToRoleType = Tuple2[Int,Int]
  class CustomeToRole(tag:Tag) extends Table[CustomerToRoleType](tag,"customers_roles") {
    def userId = column[Int]("user_id")
    def roleId = column[Int]("role_id")
    def * : ProvenShape[CustomerToRoleType] = (userId,roleId)
    def userIdFK:ForeignKeyQuery[Customers,Customer] = foreignKey("fk_user_id", userId, customers)(a => a.id)
    def roleIdFK:ForeignKeyQuery[JustRoles,Role] = foreignKey("fk_role_id", roleId, justroles)(a => a.id)
  }
}

trait RoleComponent extends WithMyDriver{
  import driver.simple._
  import models.current.dao._
  import scala.slick.lifted.{Query, ProvenShape, ForeignKeyQuery}

  class Roles(tag: Tag) extends Table[BaseRole](tag, "roles") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    def code = column[String]("code")
    def name = column[String]("name")
    def isAdmin = column[Boolean]("is_admin", O.Default(false))
    def isSuperAdmin = column[Option[Boolean]]("is_admin")
    def modelType = column[String]("model_type")

    def * :ProvenShape[BaseRole] = (id.?,code,name,isAdmin,isSuperAdmin,modelType).shaped <> ({ t => t match {
        case (a:Option[Int],b,c,d,x @ Some(true),y @ "SuperAdmin") => SuperAdmin:BaseRole
        case (a:Option[Int],b,c,d @ true,x @ None,y @ "AdminRole") => Admin(a,b,c):BaseRole
        case (a:Option[Int],b,c,d:Boolean,x @ None,y @ "Role") => Role(a,b,c,d):BaseRole
        case (a:Option[Int],b,c,d:Boolean,_,_) => Role(a,b,c,d):BaseRole
      }
    },
    {t:BaseRole => t match {
      case SuperAdmin => Some((SuperAdmin.id,SuperAdmin.code,SuperAdmin.name,SuperAdmin.isAdmin,Some(true),SuperAdmin.name))
      case Admin(a,b,c) => Some((a,b,c,true,None,"AdminRole"))
      case Role(a,b,c,d) => Some((a,b,c,d,None,"Role"))
      }
    }
    )
  }

  val roles = TableQuery[Roles]
}
