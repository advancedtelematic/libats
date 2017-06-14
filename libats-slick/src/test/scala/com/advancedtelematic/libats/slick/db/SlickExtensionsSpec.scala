package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.test.DatabaseSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSuite, Matchers}
import slick.lifted.TableQuery
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace

object SlickExtensionsSpec {
  case class Book(id: Long, title: String, code: Option[String] = None)

  class BooksTable(tag: Tag) extends Table[Book](tag, "books") {
    def id = column[Long]("id", O.PrimaryKey)
    def title = column[String]("title")
    def code = column[Option[String]]("code")

    override def * = (id, title, code) <> ((Book.apply _).tupled, Book.unapply)
  }

  protected val books = TableQuery[BooksTable]
}


class SlickExtensionsSpec extends FunSuite with Matchers with ScalaFutures with DatabaseSpec {
  import SlickExtensionsSpec._
  import SlickExtensions._

  val Error = new Exception("Expected Error") with NoStackTrace

  import ExecutionContext.Implicits.global

  override implicit def patienceConfig = PatienceConfig().copy(timeout = Span(5, Seconds))

  test("resultHead on a Query returns the first query result") {
    val f = for {
      _ <- db.run(books += Book(10, "Some book"))
      inserted <- db.run(books.resultHead(Error))
    } yield inserted

    f.futureValue shouldBe Book(10, "Some book")
  }

  test("resultHead on a Query returns the error in arg") {
    val f = db.run(books.filter(_.id === 20l).resultHead(Error))
    f.failed.futureValue shouldBe Error
  }

  test("maybeFilter uses filter if condition is defined") {
    val f = for {
      _ <- db.run(books += Book(30, "Some book", Option("30 some code")))
      result <- db.run(books.maybeFilter(_.id === Option(30l)).result)
    } yield result

    f.futureValue.length shouldBe 1
    f.futureValue.head.id shouldBe 30l
  }

  test("maybeFilter ignores filter if condition is None") {
    val f = for {
      _ <- db.run(books += Book(40, "Some book"))
      result <- db.run(books.maybeFilter(_.id === Option.empty[Long]).result)
    } yield result

    f.futureValue.length shouldBe >(1)
    f.futureValue.map(_.id) should contain(40l)
  }
}
