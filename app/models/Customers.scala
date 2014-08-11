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


case class Address(line1:String, line2:Option[String], city:String)
case class Customer(
  id: Option[Int],
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
    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)
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

    def * = (id, name, email, address, status, active, dob, interests, others, enabled, createdOn) <> (Customer.tupled, Customer.unapply)
  }
}
