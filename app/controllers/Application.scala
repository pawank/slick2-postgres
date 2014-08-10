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
  def index = DBAction { implicit rs =>
    val data = customers.list
    println(s"Data list: $data")
    Ok(views.html.index(data))
  }

  val userForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText,
      "email" -> email,
      "address" -> mapping(
      "line1" -> nonEmptyText,
      "line2" -> optional(text),
      "city" -> nonEmptyText)(Address.apply)(Address.unapply),
      "active" -> boolean,
      "dob" -> datetime,
      "interests" -> list(text),
      "others" -> optional(json),
      "createdOn" -> datetime
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
  
}
