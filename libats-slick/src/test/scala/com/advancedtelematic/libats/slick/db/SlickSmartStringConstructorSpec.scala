package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.data.{SmartStringConstructor, ValidatedString}
import com.advancedtelematic.libats.slick.db.SlickSmartConstructor._
import com.advancedtelematic.libats.test.DatabaseSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FunSuite, Matchers}
import slick.jdbc.MySQLProfile.api._

object SlickSmartStringConstructorSpec {

  case class SmartBook(id: Int, smartTitle: ValidatedString)

  class SmartBookTable(tag: Tag) extends Table[SmartBook](tag, "smart_book"){
    def id = column[Int]("id")
    def smartTitle = column[ValidatedString]("smart_title")
    override def * = (id, smartTitle) <> ((SmartBook.apply _).tupled, SmartBook.unapply)
  }

  val smartBooks = TableQuery[SmartBookTable]

}

class SlickSmartStringConstructorSpec extends FunSuite with Matchers with ScalaFutures with EitherValues with DatabaseSpec {

  import SlickSmartStringConstructorSpec._

  val b = SmartBook(1, SmartStringConstructor[ValidatedString]("Beyond good and evil").right.value)

  test("should save a StringSmartEncoder in the DB") {
    db.run(smartBooks += b).futureValue shouldBe 1
  }

  test("should fetch a StringSmartEncoder from the DB") {
    val res = db.run(smartBooks.filter(_.id === 1).result).futureValue
    res should contain only b
  }

}
