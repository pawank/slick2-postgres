package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.db.slick._
import play.api.Play.current
import myUtils._
import models._

//stable imports to use play.api.Play.current outside of objects:
import models.current.dao._
import models.current.dao.driver.simple._
import org.joda.time._

object Application extends Controller{
  /**
  * Use the default session available in `Request` object
  *
  */
  def index = DBAction { implicit rs =>
    val data = customers.list
    println(s"Data list: $data")
    Ok(views.html.index(data))
  }


  /**
  * Use another database configured in application.conf as "po" 
  *
  */
  def explicitlyUseDb = Action { implicit rs =>
    play.api.db.slick.DB("po").withSession{ implicit session =>
    val data = customers.list
    println(s"Data list: $data")
    Ok(views.html.index(data))
    }
  }

  /**
  * Directly use JDBC postgres driver
  *
  */
  def directlyUseDb = Action { implicit rs =>
    import myUtils.MyPostgresDriver._
    scala.slick.jdbc.JdbcBackend.Database.forURL("jdbc:postgresql://localhost/test", driver = "org.postgresql.Driver", user = "test", password = "testtest").withSession{ implicit session =>
    val ids = List(1)
    val data = customers.filter(_.id inSetBind ids).map(t => t).list
    println(s"Data list: $data")
    Ok(views.html.index(data))
    }
  }


  import form.form.enum
  val userForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText,
      "email" -> email,
      "address" -> mapping(
      "line1" -> nonEmptyText,
      "line2" -> optional(text),
      "city" -> nonEmptyText)(Address.apply)(Address.unapply),
      "status" -> enum(AccountStatuses),
      "active" -> boolean,
      "dob" -> datetime,
      "interests" -> list(text),
      "others" -> optional(json),
      "createdOn" -> jodaDate("yyyy-MM-dd'T'HH:mm:sssZ")
    )(Customer.apply)(Customer.unapply)
  )
  
  def insert = DBAction{ implicit rs =>
  userForm.bindFromRequest.fold(
          errors => {
            println(s"ERRORS:$errors")
          Redirect(routes.Application.index)
          },
          customer => {
    val customer = userForm.bindFromRequest.get
    println(s"Incoming customer: $customer")
    customers.insert(customer)
    Redirect(routes.Application.index)
          })
  }
  
  val profileForm = Form(
    mapping(
      "userId" -> optional(number),
      "additionalAddress" -> mapping(
      "line1" -> nonEmptyText,
      "line2" -> optional(text),
      "city" -> nonEmptyText)(Address.apply)(Address.unapply),
      "country" -> nonEmptyText
    )(MyProfile.apply)(MyProfile.unapply)
  )
  
  def profiles = DBAction { implicit rs =>
    val data = myprofiles.list
    println(s"Data list: $data")
    Ok(views.html.profile(data))
  }
  
  def addProfile = DBAction{ implicit rs =>
  profileForm.bindFromRequest.fold(
          errors => {
            println(s"ERRORS:$errors")
          Redirect(routes.Application.profiles)
          },
          my => {
    val my = profileForm.bindFromRequest.get
    println(s"Incoming profile: $my")
    myprofiles.insert(my)
    Redirect(routes.Application.profiles)
          })
  }
}
