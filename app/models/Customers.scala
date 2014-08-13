package models

import com.vividsolutions.jts.geom.Point
import org.joda.time.{LocalDateTime,DateTime}
import play.api.libs.json.JsValue

import myUtils.{WithMyDriver}
import myUtils.MyPostgresDriver.simple._

object ModelsConstants {
  val ROLE = "Role"
  val ADMIN = "Admin"
  val SUPER_ADMIN = "SuperAdmin"
}
import ModelsConstants._

trait Entity[PK] {
  def id: Option[PK]
}

trait EntityTable[I] {
  def id: scala.slick.lifted.Column[I]
}

trait Profile extends WithMyDriver {
  //val profile: scala.slick.driver.JdbcProfile
  val profile:scala.slick.driver.PostgresDriver
  val simple:profile.simple.type = profile.simple
}

trait CrudComponent extends WithMyDriver{
  import driver.simple._

    abstract class Crud[T <: Table[E] with EntityTable[PK], E <: Entity[PK], PK: BaseColumnType] {
      val query: TableQuery[T]
        private val byIdCompiled = Compiled{ (id: Column[PK]) => query.filter(_.id === id) }
        def count(implicit s: Session): Int = query.length.run
        def findAll(implicit s: Session): List[E] = query.list
        def queryById(id: PK)(implicit s: Session) = byIdCompiled(id)
        def findById(id: PK)(implicit s: Session): Option[E] = byIdCompiled(id).firstOption
        def findOne(id: PK)(implicit s: Session): Option[E] = queryById(id).firstOption
        def add(m: E)(implicit s: Session): PK = (query returning query.map(_.id)) += m
        def withId(model: E, id: PK)(implicit s: Session): E
        def extractId(m: E)(implicit s: Session): Option[PK] = m.id
        def save(m: E)(implicit s: Session): E = extractId(m) match {
          case Some(id) =>
            queryById(id).update(m)
            m
            case None => withId(m, add(m))
        }
        def saveAll(ms: E*)(implicit s: Session): Option[Int] = query ++= ms
        def deleteById(id: PK)(implicit s: Session): Int = queryById(id).delete
        def delete(m: E)(implicit s: Session): Int = extractId(m) match {
          case Some(id) => deleteById(id)
          case None => -1
        }
        def update(entity: E)(implicit s: Session): Unit      = entity.id.map{ id =>
              byIdCompiled(id).update(entity)
        }.getOrElse{
              throw new Exception("cannot update entity without id")
        }
    }
}

object AccountStatuses extends Enumeration {
  type AccountStatus = Value
  val NOT_REGISTERED, REGISTERED = Value
}
import AccountStatuses._

sealed trait Active
case object Enabled extends Active
case object Disabled extends Active

sealed trait BaseRole extends Entity[Int]{
  def code:String
  def name:String
  def isAdmin:Boolean
  def isSuperAdmin:Option[Boolean]
  def modelType:String
}

case class Role(val id:Option[Int] = None, val code:String, name:String, override val isAdmin:Boolean = false, final val isSuperAdmin:Option[Boolean] = None,final val modelType:String = ROLE) extends BaseRole
case class Admin(val id:Option[Int] = None, val code:String, name:String, override final val isAdmin:Boolean = true, final val isSuperAdmin:Option[Boolean] = None,final val modelType:String = ADMIN) extends BaseRole 
case class SuperAdmin(val id:Option[Int] = None, val code:String, name:String, override final val isAdmin:Boolean = true, final val isSuperAdmin:Option[Boolean] = Some(true),final val modelType:String = SUPER_ADMIN) extends BaseRole 


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
) extends Entity[Int]

trait CustomerComponent extends CrudComponent{
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

  class Customers(tag: Tag) extends Table[Customer](tag, "users") with EntityTable[Int] {
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

  object Customers extends Crud[Customers, Customer, Int] {
    val query = TableQuery[Customers]
    override def withId(user: Customer, id: Int)(implicit session: Session): Customer = user.copy(id = Option(id))
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
    def userIdFK:ForeignKeyQuery[Customers,Customer] = foreignKey("fk_user_id", userId, TableQuery[Customers])(a => a.id)
    def roleIdFK:ForeignKeyQuery[BaseRoles,BaseRole] = foreignKey("fk_role_id", roleId, baseroles)(a => a.id)
  }
}

trait RoleComponent extends CrudComponent{
  import driver.simple._
  import models.current.dao._
  import scala.slick.lifted.{Query, ProvenShape, ForeignKeyQuery}
  
  abstract class AbstractRoles[T](tag: Tag) extends Table[T](tag, "roles") with EntityTable[Int]{
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    def code = column[String]("code")
    def name = column[String]("name")
    def isAdmin = column[Boolean]("is_admin", O.Default(false))
    def isSuperAdmin = column[Option[Boolean]]("is_super_admin")
    def modelType = column[String]("model_type")
    def * :ProvenShape[T] 
  }

  /*
  class Roles(tag:Tag) extends AbstractRoles[Role](tag) {
    def *  = (id.?,code,name,isAdmin,isSuperAdmin.?,modelType).shaped <> (Role.tupled, Role.unapply)
  }
  object Roles extends Crud[Roles, Role, Int] {
    val query = TableQuery[Roles]
    override def withId(role: Role, id: Int)(implicit session: Session): Role = role.copy(id = Option(id))
  }
  class Admins(tag:Tag) extends AbstractRoles[Admin](tag) {
    def *  = (id.?,code,name,isAdmin,isSuperAdmin,modelType).shaped <> (Admin.tupled, Admin.unapply)
  }
  object Admins extends Crud[Admins, Admin, Int] {
    val query = TableQuery[Admins]
    override def withId(role: Admin, id: Int)(implicit session: Session): Admin = role.copy(id = Option(id))
  }
  class SuperAdmins(tag:Tag) extends AbstractRoles[SuperAdmin](tag) {
    def *  = (id.?,code,name,isAdmin,isSuperAdmin,modelType).shaped <> (SuperAdmin.tupled, SuperAdmin.unapply)
  }
  */

  class BaseRoles(tag: Tag) extends AbstractRoles[BaseRole](tag) {
    def * :ProvenShape[BaseRole] = (id.?,code,name,isAdmin,isSuperAdmin,modelType).shaped <> ({ t => t match {
        case (a:Option[Int],b,c,d,x @ Some(true),y @ SUPER_ADMIN) => SuperAdmin(a,b,c):BaseRole
        case (a:Option[Int],b,c,d @ true,x @ None,y @ ADMIN) => Admin(a,b,c):BaseRole
        case (a:Option[Int],b,c,d,e,f) => Role(a,b,c,d,e,f):BaseRole
      }
    },
    {t:BaseRole => t match {
      case SuperAdmin(a,b,c,true,Some(true),SUPER_ADMIN) => Some((a,b,c,true,Some(true),SUPER_ADMIN))
      case Admin(a,b,c,true,None,ADMIN) => Some((a,b,c,true,None,ADMIN))
      case Role(a,b,c,d,None,ROLE) => Some((a,b,c,d,None,ROLE))
      case _ => None
      }
    }
    )
  }

  object BaseRoles extends Crud[BaseRoles, BaseRole, Int] {
    val query = TableQuery[BaseRoles]
    override def withId(role: BaseRole, id: Int)(implicit session: Session): BaseRole = role match {
      case x @ SuperAdmin(_,_,_,_,_,_) => x.copy(id = Option(id))
      case x @ Admin(_,_,_,_,_,_) => x.copy(id = Option(id))
      case x @ Role(_,_,_,_,_,_) => x.copy(id = Option(id))
    }
  }
}
