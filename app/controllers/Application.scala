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
    //val data = customers.query.list
    val data = customers.list
    println(s"Data list: $data")
    //println(s"No of customers: ${customers.query.count}")
    Ok(views.html.index(data))
  }


  /**
  * Use another database configured in application.conf as "po" 
  *
  */
  def explicitlyUseDb = Action { implicit rs =>
    play.api.db.slick.DB("po").withSession{ implicit session =>
    //val data = customers.query.list
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
    //val data = customers.query.filter(_.id inSetBind ids).map(t => t).list
    val data = customers.filter(_.id inSetBind ids).map(t => t).list
    println(s"Data list: $data")
    Ok(views.html.index(data))
    }
  }

  /**
  * Use AuthenticatedCustomerAction action having db session and customer object extracted from incoming request for querying db
  *
  */
  def usingAuthenticatedCustomerAction = AuthenticatedCustomerAction.async { implicit rs =>
    import play.api.libs.concurrent.Execution.Implicits._
    rs.dbSession.withSession{ implicit session =>
    val ids = List(1)
    //val data = customers.query.filter(_.id inSetBind ids).map(t => t).list
    val data = customers.filter(_.id inSetBind ids).map(t => t).list
    println(s"usingAuthenticatedCustomerAction: Data list: $data")
    scala.concurrent.Future {
      Ok(views.html.index(data))
    }
    }
  }

  object EnabledMappings {
  import play.api.data.format.Formatter
  import play.api.data.Mapping
  import play.api.data.Forms
  import play.api.data.FormError
  import play.api.data.FormError

  implicit val enabledFormat = new Formatter[Active] {
    private def createErrorMsg(key: String, msg: String) = Left(List(new FormError(key, msg)))
    def bind(key: String, data: Map[String, String]):Either[Seq[FormError], Active] = {
      data.get(key).map { value =>
        try {
          value match {
            case "true" => Right(Enabled)
            case "1" => Right(Enabled)
            case "True" => Right(Enabled)
            case _ => Right(Disabled)
          }
        } catch {
          case e: NoSuchElementException => createErrorMsg(key, "Active is not a valid type")
        }
      }.getOrElse(createErrorMsg(key, "No `Active` type is provided"))
    }
    def unbind(key: String, value: Active) = Map(key -> value.toString)
  }

    def enabled: Mapping[Active] = Forms.of[Active]
  }

  import EnabledMappings._

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
      //"enabled" -> of[Active],
      "enabled" -> enabled, //shortcut for Active type
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
      "admin" -> mapping(
        "adminType" -> nonEmptyText,
        "isAdmin" -> boolean
      )((a,b) => Pair[String,Boolean](a,b))((x:Pair[String,Boolean]) => Some(x._1,x._2)),
      "country" -> nonEmptyText
    )(MyProfile.apply)(MyProfile.unapply)
  )
  
  def profiles = DBAction { implicit rs =>
    val data = myprofiles.list
    println(s"Data list: $data")
    Ok(views.html.profile(data))
  }
  
  def profilesQuery = DBAction { implicit rs =>
    val profileQuery: Query[MyProfiles,MyProfile,Seq] = myprofiles.filter(_.userId inSet(Set(2,3)))
    val data = profileQuery.run
    println(s"Data list: $data")
    Ok(views.html.profile(data.toList))
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
