package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.data.{ValidatedGeneric, ValidationError}
import com.advancedtelematic.libats.test.DatabaseSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FunSuite, Matchers}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace

object SlickExtensionsSpec {
  import SlickValidatedGeneric.validatedGenericMapper

  final case class BookTitle(title: String) extends AnyVal
  object BookTitle {
    implicit val validatedBookTitle: ValidatedGeneric[BookTitle, String] = new ValidatedGeneric[BookTitle, String] {
      override def to(t: BookTitle): String = t.title

      override def from(r: String): Either[ValidationError, BookTitle] =
        if (r contains "invalid") Left(ValidationError("The book title is invalid."))
        else Right(new BookTitle(r))
    }
  }

  case class Book(id: Long, title: BookTitle, code: Option[String] = None)

  class BooksTable(tag: Tag) extends Table[Book](tag, "books") {
    def id = column[Long]("id", O.PrimaryKey)
    def title = column[BookTitle]("title")
    def code = column[Option[String]]("code")

    override def * = (id, title, code) <> ((Book.apply _).tupled, Book.unapply)
  }

  protected val books = TableQuery[BooksTable]

  case class BookMeta(id: Long, bookId: Long, tag: Long)

  class BookMetaTable(tag: Tag) extends Table[BookMeta](tag, "book_meta") {
    def id    = column[Long]("id")
    def bookId = column[Long]("book_id")
    def bookTag  = column[Long]("tag")

    def pk = primaryKey("book_meta_pk", (bookId, id))

    override def * = (bookId, id, bookTag) <> ((BookMeta.apply _).tupled, BookMeta.unapply)
  }

  protected val bookMeta = TableQuery[BookMetaTable]
}


class SlickExtensionsSpec extends FunSuite with Matchers with ScalaFutures with DatabaseSpec {
  import SlickExtensions._
  import SlickExtensionsSpec._
  import SlickExtensionsSpec.BookTitle.validatedBookTitle

  val Error = new Exception("Expected Error") with NoStackTrace

  import ExecutionContext.Implicits.global

  override implicit def patienceConfig = PatienceConfig().copy(timeout = Span(5, Seconds))

  test("resultHead on a Query returns the first query result") {
    val f = for {
      _ <- db.run(books += Book(10, validatedBookTitle.from("Some book").right.get))
      inserted <- db.run(books.resultHead(Error))
    } yield inserted

    f.futureValue shouldBe Book(10, validatedBookTitle.from("Some book").right.get)
  }

  test("resultHead on a Query returns the error in arg") {
    val f = db.run(books.filter(_.id === 15l).resultHead(Error))
    f.failed.futureValue shouldBe Error
  }

  test("maybeFilter uses filter if condition is defined") {
    val f = for {
      _ <- db.run(books += Book(20, validatedBookTitle.from("Some book").right.get, Option("20 some code")))
      result <- db.run(books.maybeFilter(_.id === Option(20l)).result)
    } yield result

    f.futureValue.length shouldBe 1
    f.futureValue.head.id shouldBe 20l
  }

  test("maybeFilter ignores filter if condition is None") {
    val f = for {
      _ <- db.run(books += Book(30, validatedBookTitle.from("Some book").right.get))
      result <- db.run(books.maybeFilter(_.id === Option.empty[Long]).result)
    } yield result

    f.futureValue.length shouldBe >(1)
    f.futureValue.map(_.id) should contain(30l)
  }

  test("maybeContains uses string if it is defined") {
    val f = for {
      _ <- db.run(books += Book(40, validatedBookTitle.from("A very interesting book").right.get, Some("30 some code")))
      result <- db.run(books.maybeContains(_.title, Some("interesting")).result)
    } yield result

    f.futureValue.length shouldBe 1
    f.futureValue.head.id shouldBe 40l
  }

  test("maybeContains gives all elements if string is empty") {
    val result = db.run(books.maybeContains(_.title, Some("")).result)
    result.futureValue.length shouldBe 4
  }

  test("maybeContains gives all elements if string is None") {
    val result = db.run(books.maybeContains(_.title, None).result)
    result.futureValue.length shouldBe 4
  }

  test("handleIntegrityErrors works with mariadb 10.2") {
    val g = BookMeta(-1, -1, 0)
    val f = db.run(bookMeta.insertOrUpdate(g).handleIntegrityErrors(Error))

    f.failed.futureValue shouldBe Error
  }

  test("handleForeignKeyError throws the expected error") {
    val g = BookMeta(1, 1984, 0)
    val f = db.run((bookMeta += g).handleForeignKeyError(Error))

    f.failed.futureValue shouldBe Error
  }

  test("handleForeignKeyError ignores the error when FK exists") {
    val b = Book(15, validatedBookTitle.from("The Count of Monte Cristo").right.get, Some("9781377261379"))
    val bm = BookMeta(15, 15, 0)
    val f = db.run((books += b).andThen(bookMeta += bm).handleForeignKeyError(Error))

    f.futureValue shouldBe 1
  }
}
