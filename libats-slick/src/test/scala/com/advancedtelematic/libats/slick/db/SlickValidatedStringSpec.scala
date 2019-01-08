package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.data.{ValidatedString, ValidatedStringConstructor}
import com.advancedtelematic.libats.slick.db.SlickValidatedString._
import com.advancedtelematic.libats.test.DatabaseSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FunSuite, Matchers}
import slick.jdbc.MySQLProfile.api._

object SlickValidatedStringSpec {

  case class SmartBook(id: Int, smartTitle: ValidatedString)

  class SmartBookTable(tag: Tag) extends Table[SmartBook](tag, "smart_book"){
    def id = column[Int]("id")
    def smartTitle = column[ValidatedString]("smart_title")
    override def * = (id, smartTitle) <> ((SmartBook.apply _).tupled, SmartBook.unapply)
  }

  val smartBooks = TableQuery[SmartBookTable]

}

class SlickValidatedStringSpec extends FunSuite with Matchers with EitherValues with ScalaFutures with DatabaseSpec {

  import SlickValidatedStringSpec._

  val b = SmartBook(1, ValidatedStringConstructor[ValidatedString]("Beyond good and evil").right.value)

  test("should save a ValidatedString in the DB") {
    db.run(smartBooks += b).futureValue shouldBe 1
  }

  test("should fetch a ValidatedString from the DB") {
    val res = db.run(smartBooks.filter(_.id === 1).result).futureValue
    res should contain only b
  }

}
