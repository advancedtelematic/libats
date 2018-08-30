/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.slick.db

import java.sql.{BatchUpdateException, SQLException, SQLIntegrityConstraintViolationException, Timestamp}
import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.Uri
import com.advancedtelematic.libats.data.PaginationResult
import com.advancedtelematic.libats.http.Errors
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import slick.ast.{Node, TypedType}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{AbstractTable, Rep}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.util.{Failure, Success}
import scala.language.implicitConversions


object SlickPipeToUnit {
  implicit def pipeToUnit(value: DBIO[Any])(implicit ec: ExecutionContext): DBIO[Unit] = value.map(_ => ())
}

trait SlickResultExtensions {
  implicit class DBIOActionExtensions[T](action: DBIO[T]) {

    def handleForeignKeyError(error: Throwable)(implicit ec: ExecutionContext): DBIO[T] = {
      action.asTry.flatMap {
        case Success(a) => DBIO.successful(a)
        case Failure(e: SQLIntegrityConstraintViolationException) if e.getErrorCode == 1452 => DBIO.failed(error) // ER_NO_REFERENCED_ROW_2
        case Failure(e) => DBIO.failed(e)
      }
    }

    def handleIntegrityErrors(error: Throwable)(implicit ec: ExecutionContext): DBIO[T] = {
      action.asTry.flatMap {
        case Success(i) =>
          DBIO.successful(i)
        case Failure(e: SQLIntegrityConstraintViolationException) =>
          DBIO.failed(error)
        case Failure(e: BatchUpdateException) if e.getCause.isInstanceOf[SQLIntegrityConstraintViolationException] =>
          DBIO.failed(error)
        case Failure(e: SQLException) if e.getErrorCode == 1032 => // ER_KEY_NOT_FOUND See https://mariadb.com/kb/en/library/mariadb-error-codes/
          DBIO.failed(error)
        case Failure(e) =>
          DBIO.failed(e)
      }
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
      query.withFilter { (e: E) =>
        f(e).getOrElse(true)
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
    def paginateAndSort[T <% slick.lifted.Ordered](fn: E => T, offset: Long, limit: Long): Query[E, U, Seq] = {
      action
        .sortBy(fn)
        .drop(offset)
        .take(limit)
    }

    def paginate(offset: Long, limit: Long): Query[E, U, Seq] = {
      action
        .drop(offset)
        .take(limit)
    }

    def paginateAndSortResult[T <% slick.lifted.Ordered](fn: E => T, offset: Long, limit: Long)
                                                        (implicit ec: ExecutionContext): DBIO[PaginationResult[U]] = {
      val tot = action.length.result
      val pag = action.paginateAndSort(fn, offset, limit).result

      tot.zip(pag).map{ case (total, values) =>
        PaginationResult(total = total, limit = limit, offset = offset, values = values)
      }
    }

    def paginateResult(offset: Long, limit: Long)(implicit ec: ExecutionContext): DBIO[PaginationResult[U]] = {
      val tot = action.length.result
      val pag = action.paginate(offset, limit).result

      tot.zip(pag).map{ case (total, values) =>
        PaginationResult(total = total, limit = limit, offset = offset, values = values)
      }
    }
  }
}

object SlickPagination extends SlickPagination

object SlickExtensions extends SlickResultExtensions with SlickPagination {
  implicit val UriColumnType = MappedColumnType.base[Uri, String](_.toString(), Uri.apply)

  implicit val uuidColumnType = MappedColumnType.base[UUID, String]( _.toString(), UUID.fromString )

  implicit val javaInstantMapping = {
    MappedColumnType.base[Instant, Timestamp](
      dt => Timestamp.from(dt),
      ts => ts.toInstant)
  }

  final class MappedExtensionMethods(val n: Node) extends AnyVal {

    def mappedTo[U: TypedType] = Rep.forNode[U](n)

  }

  implicit def mappedColumnExtensions(c: Rep[_]) : MappedExtensionMethods = new MappedExtensionMethods(c.toNode)

  implicit def uuidToJava(refined: Refined[String, Uuid]): Rep[UUID] =
    UUID.fromString(refined.value).bind

  implicit class InsertOrUpdateWithKeyOps[Q <: AbstractTable[_], E](tableQuery: TableQuery[Q])
                                                                   (implicit ev: E =:= Q#TableElementType) {

    def insertOrUpdateWithKey(element: E,
                              primaryKeyQuery: TableQuery[Q] => Query[Q, E, Seq],
                              onUpdate: E => E
                             )(implicit ec: ExecutionContext): DBIO[E] = {

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
