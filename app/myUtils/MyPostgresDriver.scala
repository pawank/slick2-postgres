package myUtils

import com.github.tminglei.slickpg._
import slick.driver.PostgresDriver

import models.AccountStatuses

trait WithMyDriver {
  val driver: MyPostgresDriver
}

////////////////////////////////////////////////////////////
trait MyPostgresDriver extends PostgresDriver
                          with PgArraySupport
                          with PgDateSupportJoda
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgPlayJsonSupport
                          with PgSearchSupport
                          with PgEnumSupport
                          with PgPostGISSupport {

  //override lazy val Implicit = new ImplicitsPlus {}
  //override val simple = new SimpleQLPlus {}
  
  override lazy val Implicit = new ImplicitsPlus with MyEnumImplicits {}
  override val simple = new SimpleQLPlus with MyEnumImplicits {}
  trait MyEnumImplicits {
    implicit val accountStatusTypeMapper = createEnumJdbcType("account_status", AccountStatuses)
      implicit val accountStatusListTypeMapper = createEnumListJdbcType("account_status", AccountStatuses)

      implicit val accountStatusColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(AccountStatuses)
      implicit val accountStatusOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder(AccountStatuses)
  }
  
  //////
  trait ImplicitsPlus extends Implicits
                        with ArrayImplicits
                        with DateTimeImplicits
                        with RangeImplicits
                        with HStoreImplicits
                        with JsonImplicits
                        with SearchImplicits
                        with PostGISImplicits

  trait SimpleQLPlus extends SimpleQL
                        with ImplicitsPlus
                        with SearchAssistants
                        with PostGISAssistants

}

object MyPostgresDriver extends MyPostgresDriver
