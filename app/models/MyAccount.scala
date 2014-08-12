package models

import org.joda.time.{LocalDateTime,DateTime}
import play.api.libs.json.JsValue
import myUtils.{WithMyDriver}

import models._
case class MyProfile(
  userId: Option[Int],
  additionalAddress:Address,
  admin:Pair[String,Boolean],
  country:String
)

case class Pair[A, B](_1: A, _2: B)

trait MyProfileComponent extends WithMyDriver{
  import driver.simple._

  import scala.slick.lifted.{ProvenShape, ForeignKeyQuery}

  /**
  * Creating a custom class as type to be used in slick
  * More info: http://slick.typesafe.com/doc/2.1.0-M2/userdefined.html
  */
  import scala.reflect.ClassTag
  import scala.slick.lifted.MappedScalaProductShape
// A Shape implementation for Pair
final class PairShape[Level <: ShapeLevel, M <: Pair[_,_], U <: Pair[_,_] : ClassTag, P <: Pair[_,_]](
      val shapes: Seq[Shape[_, _, _, _]])
  extends MappedScalaProductShape[Level, Pair[_,_], M, U, P] {
    def buildValue(elems: IndexedSeq[Any]) = Pair(elems(0), elems(1))
      def copy(shapes: Seq[Shape[_ <: ShapeLevel, _, _, _]]) = new PairShape(shapes)
}

implicit def pairShape[Level <: ShapeLevel, M1, M2, U1, U2, P1, P2](
    implicit s1: Shape[_ <: Level, M1, U1, P1], s2: Shape[_ <: Level, M2, U2, P2]
    ) = new PairShape[Level, Pair[M1, M2], Pair[U1, U2], Pair[P1, P2]](Seq(s1, s2))
   //End of Pair custom class implicits 

  class MyProfiles(tag: Tag) extends Table[MyProfile](tag, "profiles") {
      def userId = column[Option[Int]]("user_id")
      def line1 = column[String]("line1")
      def line2 = column[Option[String]]("line2")
      def city = column[String]("city")
      def adminType = column[String]("admin_type")
      def isAdmin = column[Boolean]("is_admin")
      def country = column[String]("country")
      import models.current.dao._
      def customer:ForeignKeyQuery[Customers,Customer] = foreignKey("fk_user_id", userId, TableQuery[Customers])(_.id)

    def * = (
      userId,
      (line1, line2, city),
      (adminType, isAdmin),
      country
    ).shaped <> ({ case (userId, address, admin, country) => 
      MyProfile(userId, Address.tupled.apply(address), Pair(admin._1, admin._2), country)
      }, {my:MyProfile => 
        def f(ad:Address) = Address.unapply(ad).get
        def fpair(p:Pair[String,Boolean]) = Pair.unapply(p).get
        Some((my.userId, f(my.additionalAddress), fpair(my.admin), my.country))
      }
    )
  }
}
