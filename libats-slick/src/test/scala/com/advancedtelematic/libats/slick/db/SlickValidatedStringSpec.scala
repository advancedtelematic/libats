package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.data.ValidatedStringConstructor
import com.advancedtelematic.libats.slick.db.SlickValidatedString._
import com.advancedtelematic.libats.test.{DatabaseSpec, ValidatedStringTest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FunSuite, Matchers}
import slick.jdbc.MySQLProfile.api._

object SlickValidatedStringSpec {

  case class ValidatedBook(id: Int, validatedTitle: ValidatedStringTest)

  class ValidatedBookTable(tag: Tag) extends Table[ValidatedBook](tag, "validated_book"){
    def id = column[Int]("id")
    def validatedTitle = column[ValidatedStringTest]("validated_title")
    override def * = (id, validatedTitle) <> ((ValidatedBook.apply _).tupled, ValidatedBook.unapply)
  }

  val validatedBooks = TableQuery[ValidatedBookTable]

}

class SlickValidatedStringSpec extends FunSuite with Matchers with EitherValues with ScalaFutures with DatabaseSpec {

  import SlickValidatedStringSpec._

  val b = ValidatedBook(1, ValidatedStringConstructor[ValidatedStringTest]("Beyond good and evil").right.value)

  test("should save a ValidatedString in the DB") {
    db.run(validatedBooks += b).futureValue shouldBe 1
  }

  test("should fetch a ValidatedString from the DB") {
    val res = db.run(validatedBooks.filter(_.id === 1).result).futureValue
    res should contain only b
  }

}
