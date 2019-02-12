package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.slick.db.SqlExceptions.{NoReferencedRow, IntegrityConstraintViolationError}
import com.advancedtelematic.libats.test.DatabaseSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FunSuite, Matchers}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

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

  case class BookMeta(id: Long, bookId: Long, tag: Long)

  class BookMetaTable(tag: Tag) extends Table[BookMeta](tag, "book_meta") {
    def id    = column[Long]("id")
    def bookId = column[Long]("book_id")
    def bookTag  = column[Long]("tag")

    def pk = primaryKey("book_meta_pk", (bookId, id))

    override def * = (bookId, id, bookTag) <> ((BookMeta.apply _).tupled, BookMeta.unapply)
  }

  protected val bookMeta = TableQuery[BookMetaTable]

  case class BookTag(tag: Int)

  class BookTagTable(tag: Tag) extends Table[BookTag](tag, "book_tags") {
    def bookTag = column[Int]("tag")

    override def * = bookTag <> (BookTag.apply, BookTag.unapply)
  }

  protected val bookTags = TableQuery[BookTagTable]
}


class SlickExtensionsSpec extends FunSuite with Matchers with ScalaFutures with DatabaseSpec {
  import SlickExtensions._
  import SlickExtensionsSpec._

  val Error = new Exception("Expected Error") with NoStackTrace
  val ErrorBookIdFk = new Exception("book_id_fk error") with NoStackTrace
  val ErrorBookTagFk = new Exception("book_tag_fk error") with NoStackTrace

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
    val f = db.run(books.filter(_.id === 15l).resultHead(Error))
    f.failed.futureValue shouldBe Error
  }

  test("maybeFilter uses filter if condition is defined") {
    val f = for {
      _ <- db.run(books += Book(20, "Some book", Option("20 some code")))
      result <- db.run(books.maybeFilter(_.id === Option(20l)).result)
    } yield result

    f.futureValue.length shouldBe 1
    f.futureValue.head.id shouldBe 20l
  }

  test("maybeFilter ignores filter if condition is None") {
    val f = for {
      _ <- db.run(books += Book(30, "Some book"))
      result <- db.run(books.maybeFilter(_.id === Option.empty[Long]).result)
    } yield result

    f.futureValue.length shouldBe >(1)
    f.futureValue.map(_.id) should contain(30l)
  }

  test("maybeContains uses string if it is defined") {
    val f = for {
      _ <- db.run(books += Book(40, "A very interesting book", Some("30 some code")))
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

  test("handleForeignKeyError throws the expected error if one FK is missing") {
    val b = Book(50, "Beyond Good and Evil", Some("81781857261933"))
    val bm = BookMeta(b.id, 51, 0)
    val f = db.run {
      (books += b).andThen(bookMeta += bm).handleForeignKeyError(Error, {
        case NoReferencedRow("book_id_fk") => ErrorBookIdFk
        case NoReferencedRow("book_tag_fk") => ErrorBookTagFk
      })
    }

    f.failed.futureValue shouldBe ErrorBookTagFk
  }

  test("handleForeignKeyError throws the expected error if the other FK is missing") {
    val t = BookTag(1)
    val bm = BookMeta(60, 61, t.tag)
    val f = db.run {
      (bookTags += t).andThen(bookMeta += bm).handleForeignKeyError(Error, {
        case NoReferencedRow("book_id_fk") => ErrorBookIdFk
        case NoReferencedRow("book_tag_fk") => ErrorBookTagFk
      })
    }

    f.failed.futureValue shouldBe ErrorBookIdFk
  }

  test("handleForeignKeyError ignores the error when FK exists") {
    val t = BookTag(2)
    val b = Book(70, "The Count of Monte Cristo", Some("9781377261379"))
    val bm = BookMeta(b.id, 71, t.tag)
    val f = db.run {
      (books += b).andThen(bookTags += t).andThen(bookMeta += bm).handleForeignKeyError(Error)
    }

    f.futureValue shouldBe 1
  }
}
