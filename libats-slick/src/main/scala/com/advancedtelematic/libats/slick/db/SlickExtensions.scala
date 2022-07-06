/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.slick.db

import java.sql.{BatchUpdateException, SQLException, SQLIntegrityConstraintViolationException, Timestamp}
import java.time.Instant
import java.util.UUID
import akka.http.scaladsl.model.Uri
import com.advancedtelematic.libats.data.{Limit, Offset, PaginationResult}
import com.advancedtelematic.libats.http.Errors
import com.advancedtelematic.libats.slick.db.SlickExtensions.MappedColumnExtensions
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import slick.ast.TypedType
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{AbstractTable, Rep}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}


object SlickPipeToUnit {
  implicit def pipeToUnit(value: DBIO[Any])(implicit ec: ExecutionContext): DBIO[Unit] = value.map(_ => ())
}

object SqlExceptions {

  object NoReferencedRow {
    def unapply(t: Throwable): Option[SQLIntegrityConstraintViolationException] = t match {
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 => Some(e)
      case _ => None
    }
  }

  // ER_KEY_NOT_FOUND See https://mariadb.com/kb/en/library/mariadb-error-codes/
  object KeyNotFound {
    def unapply(arg: Throwable): Option[SQLException] = arg match {
      case e: SQLException if e.getErrorCode == 1032 => Some(e)
      case _ => None
    }
  }

  object IntegrityConstraintViolation {
    def unapply(arg: Throwable): Option[SQLIntegrityConstraintViolationException] = arg match {
      case e: SQLIntegrityConstraintViolationException =>
        Some(e)

      case e: BatchUpdateException if e.getCause.isInstanceOf[SQLIntegrityConstraintViolationException] =>
        Some(e.getCause.asInstanceOf[SQLIntegrityConstraintViolationException])

      case _ => None
    }
  }
}

trait SlickResultExtensions {
  implicit class DBIOActionExtensions[T](action: DBIO[T]) {
    import SqlExceptions._

    def mapError(mapping: PartialFunction[Throwable, Throwable])(implicit ec: ExecutionContext): DBIO[T] =
      recover {
        case Failure(t) if mapping.isDefinedAt(t) => DBIO.failed(mapping.apply(t))
      }

    def recover(handler: PartialFunction[Try[T], DBIO[T]])(implicit ec: ExecutionContext): DBIO[T] =
      action.asTry.flatMap{ x =>
        handler.applyOrElse(x, (t: Try[T]) => t match {
          case Success(a) => DBIO.successful(a)
          case Failure(e) => DBIO.failed(e)
        })
      }

    def handleForeignKeyError(error: Throwable)(implicit ec: ExecutionContext): DBIO[T] =
      mapError { case NoReferencedRow(_) => error }

    def handleIntegrityErrors(error: Throwable)(implicit ec: ExecutionContext): DBIO[T] =
      mapError {
        case IntegrityConstraintViolation(_) => error
        case KeyNotFound(_) => error
    }
  }

  implicit class DbioUpdateActionExtensions(action: DBIO[Int]) {
    def handleSingleUpdateError(result: Throwable)(implicit ec: ExecutionContext): DBIO[Unit] = {
      action.flatMap {
        case c if c == 1 =>
          DBIO.successful(())
        case c if c == 0 =>
          DBIO.failed(result)
        case _ =>
          DBIO.failed(Errors.TooManyElements)
      }
    }
  }

  implicit class DBIOOptionOps[T](io: DBIO[Option[T]]) {
    def failIfNone(t: Throwable)
                  (implicit ec: ExecutionContext): DBIO[T] =
      io.flatMap(_.fold[DBIO[T]](DBIO.failed(t))(DBIO.successful))
  }

  implicit class QueryOps[+E, U](query: Query[E, U, Seq]) {
    def resultHead(onEmpty: Throwable)(implicit ec: ExecutionContext): DBIO[U] =
      DBIOOptionOps(query.take(1).result.headOption).failIfNone(onEmpty)

    def maybeFilter(f: E => Rep[Option[Boolean]]): Query[E, U, Seq] =
      query.withFilter { e: E =>
        f(e).getOrElse(true)
      }

    def maybeContains(f: E => Rep[_], exp: Option[String]): Query[E, U, Seq] =
      query.withFilter { e: E =>
        exp match {
          case Some(s) if s.nonEmpty => f(e).mappedTo[String].like(s"%$s%")
          case _ => true.bind
        }
      }
  }

  implicit class DBIOSeqOps[+T](io: DBIO[Seq[T]]) {
    def failIfMany(implicit ec: ExecutionContext): DBIO[Option[T]] =
      io.flatMap { result =>
        if(result.size > 1)
          DBIO.failed(Errors.TooManyElements)
        else
          DBIO.successful(result.headOption)
      }

    def failIfNotSingle(t: Throwable)
                       (implicit ec: ExecutionContext): DBIO[T] =
      DBIOOptionOps(failIfMany).failIfNone(t)

    def failIfEmpty(t: Throwable)
                   (implicit ec: ExecutionContext): DBIO[Seq[T]] = {
      io.flatMap { result =>
        if(result.isEmpty)
          DBIO.failed(t)
        else
          DBIO.successful(result)
      }
    }
  }
}

object SlickResultExtensions extends SlickResultExtensions

trait SlickPagination {
  implicit class DBIOPaginateExtensions[E, U](action: Query[E, U, Seq]) {
    def paginate(offset: Offset, limit: Limit): Query[E, U, Seq] =
      action
        .drop(offset.value)
        .take(limit.value)

    def paginateAndSort[T <% slick.lifted.Ordered](fn: E => T, offset: Offset, limit: Limit): Query[E, U, Seq] =
      action
        .sortBy(fn)
        .drop(offset.value)
        .take(limit.value)

    def paginateResult(offset: Offset, limit: Limit)(implicit ec: ExecutionContext): DBIO[PaginationResult[U]] = {
      val tot = action.length.result
      val pag = action.paginate(offset, limit).result
      tot.zip(pag).map{ case (total, values) => PaginationResult(values, total, offset, limit) }
    }

    def paginateAndSortResult[T <% slick.lifted.Ordered](fn: E => T, offset: Offset, limit: Limit)
                                                        (implicit ec: ExecutionContext): DBIO[PaginationResult[U]] = {
      val tot = action.length.result
      val pag = action.paginateAndSort(fn, offset, limit).result
      tot.zip(pag).map{ case (total, values) => PaginationResult(values, total, offset, limit) }
    }
  }
}

object SlickPagination extends SlickPagination

object SlickExtensions extends SlickResultExtensions with SlickPagination {
  implicit val UriColumnType = MappedColumnType.base[Uri, String](_.toString(), Uri.apply)

  implicit val uuidColumnType = MappedColumnType.base[UUID, String](_.toString(), UUID.fromString)

  implicit val javaInstantMapping = MappedColumnType.base[Instant, Timestamp](Timestamp.from, _.toInstant)

  implicit class MappedColumnExtensions(c: Rep[_]) {
    def mappedTo[U: TypedType] = Rep.forNode[U](c.toNode)
  }

  implicit def uuidToJava(refined: Refined[String, Uuid]): Rep[UUID] = UUID.fromString(refined.value).bind

  implicit class InsertOrUpdateWithKeyOps[Q <: AbstractTable[_]](tableQuery: TableQuery[Q])(implicit ec: ExecutionContext) {

    type E = Q#TableElementType

    def insertIfNotExists(element: E)(findElement: TableQuery[Q] => Query[Q, E, Seq]): DBIO[Unit] =
      findElement(tableQuery).exists.result.flatMap {
        case true => DBIO.successful(())
        case false => (tableQuery += element).map(_ => ())
      }

    def insertOrUpdateWithKey(element: E, primaryKeyQuery: TableQuery[Q] => Query[Q, E, Seq], onUpdate: E => E)
                             (implicit ec: ExecutionContext): DBIO[E] = {

      val findQuery = primaryKeyQuery(tableQuery)

      def update(v: E): DBIO[E] = {
        val updated = onUpdate(v)
        findQuery.update(updated).map(_ => updated)
      }

      val io = findQuery.result.flatMap { res =>
        if(res.isEmpty)
          (tableQuery += element).map(_ => element)
        else if(res.size == 1)
          update(res.head)
        else
          DBIO.failed(new Exception("Too many elements found to update. primaryKeyQuery must define a unique key"))
      }

      io.transactionally
    }
  }
}
