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

case class Address(line1:String, line2:Option[String], city:String)
case class Customer(
  id: Option[Int] = None,
  name:String,
  email:String,
  address:Address,
  status:AccountStatus,
  active:Boolean,
  dob: LocalDateTime,
  interests: List[String],
  others: Option[JsValue],
  createdOn:DateTime
)

trait CustomerComponent extends WithMyDriver{
  import driver.simple._

  class Customers(tag: Tag) extends Table[Customer](tag, "users") {
    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def email = column[String]("email")
    def line1 = column[String]("line1")
    def line2 = column[Option[String]]("line2")
    def city = column[String]("city")
    def address = (line1,line2,city) <> (Address.tupled,Address.unapply)
    def status = column[AccountStatus]("status",O.Default(NOT_REGISTERED))
    def active = column[Boolean]("active")
    def dob = column[LocalDateTime]("dob")
    def interests = column[List[String]]("interests")
    def others = column[Option[JsValue]]("others")
    def createdOn = column[DateTime]("created_on")

    def * = (id, name, email, address, status, active, dob, interests, others, createdOn) <> (Customer.tupled, Customer.unapply)
  }
}
