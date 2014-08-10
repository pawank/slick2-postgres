package models

import com.vividsolutions.jts.geom.Point
import org.joda.time.{LocalDateTime,DateTime}
import play.api.libs.json.JsValue
import myUtils.{WithMyDriver}

import models._
case class MyProfile(
  userId: Option[Int],
  additionalAddress:Address,
  country:String
)

trait MyProfileComponent extends WithMyDriver{
  import driver.simple._

  class MyProfiles(tag: Tag) extends Table[MyProfile](tag, "profiles") {
      def userId = column[Option[Int]]("user_id")
      def line1 = column[String]("line1")
      def line2 = column[Option[String]]("line2")
      def city = column[String]("city")
      def country = column[String]("country")
      import models.current.dao._
      def customer = foreignKey("fk_user_id", userId, customers)(_.id)

    def * = (
      userId,
      (line1, line2, city),
      country
    ).shaped <> ({ case (userId, address, country) => 
      MyProfile(userId, Address.tupled.apply(address), country)
      }, {my:MyProfile => 
        def f(ad:Address) = Address.unapply(ad).get
        Some((my.userId, f(my.additionalAddress), my.country))
      }
    )
  }
}
