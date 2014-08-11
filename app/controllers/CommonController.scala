package controllers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.Routes
import play.api.mvc._
import play.api.mvc.Results._

case class ApplicationCustomer(url:String, hostname:String, db:String, username:String, password:String, uid:String)
case class AuthenticatedCustomer[A](customer:ApplicationCustomer, dbSession:scala.slick.jdbc.JdbcBackend.Database, request: Request[A]) extends WrappedRequest[A](request)
object AuthenticatedCustomerAction extends ActionBuilder[AuthenticatedCustomer] {
    def makeJdbcUrl(host:String, db:String) = s"jdbc:postgresql://$host/$db" 
    def invokeBlock[A](request: Request[A], block: (AuthenticatedCustomer[A]) => Future[Result]) = {
      //FIXME: Get the ApplicationCustomer by querying the underlying data storage system
      val maybeCustomer:Option[ApplicationCustomer] = Some(ApplicationCustomer(url = "http://customer.app.com", hostname = "localhost", db = "test", username = "test", password = "testtest", uid = "admin@app.com"))
      maybeCustomer.map{customer => 
        import myUtils.MyPostgresDriver._
        val db = scala.slick.jdbc.JdbcBackend.Database.forURL(makeJdbcUrl(customer.hostname, customer.db), driver = "org.postgresql.Driver", user = customer.username, password = customer.password)
        block(AuthenticatedCustomer(customer,db,request))
      }.getOrElse(Future.successful(Forbidden))
    }
}
