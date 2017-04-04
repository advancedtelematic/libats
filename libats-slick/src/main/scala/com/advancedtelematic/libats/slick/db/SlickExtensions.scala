/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.slick.db

import java.sql.{BatchUpdateException, SQLIntegrityConstraintViolationException, Timestamp}
import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.Uri
import com.advancedtelematic.libats.data.PaginationResult
import com.advancedtelematic.libats.http.Errors
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import slick.ast.{Node, TypedType}
import slick.driver.MySQLDriver.api._
import slick.lifted.{AbstractTable, Rep}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.util.{Failure, Success}

object SlickPipeToUnit {
  // TODO: Do not use this, still figuring out if this is a good idea
  implicit def pipeToUnit(value: DBIO[Any])(implicit ec: ExecutionContext): DBIO[Unit] = value.map(_ => ())
}

object SlickExtensions {
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

  import scala.language.implicitConversions

  implicit def mappedColumnExtensions(c: Rep[_]) : MappedExtensionMethods = new MappedExtensionMethods(c.toNode)

  implicit def uuidToJava(refined: Refined[String, Uuid]): Rep[UUID] =
    UUID.fromString(refined.get).bind

  implicit class DbioPaginateExtensions[E, U](action: Query[E, U, Seq]) {
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

  implicit class DbioActionExtensions[T](action: DBIO[T]) {
    def handleIntegrityErrors(error: Throwable)(implicit ec: ExecutionContext): DBIO[T] = {
      action.asTry.flatMap {
        case Success(i) =>
          DBIO.successful(i)
        case Failure(e: SQLIntegrityConstraintViolationException) =>
          DBIO.failed(error)
        case Failure(e: BatchUpdateException) if e.getCause.isInstanceOf[SQLIntegrityConstraintViolationException] =>
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

  implicit class DBIOOps[T](io: DBIO[Option[T]]) {

    def failIfNone(t: Throwable)
                  (implicit ec: ExecutionContext): DBIO[T] =
      io.flatMap(_.fold[DBIO[T]](DBIO.failed(t))(DBIO.successful))
  }

  implicit class DBIOSeqOps[+T](io: DBIO[Seq[T]]) {
    def failIfMany()(implicit ec: ExecutionContext): DBIO[Option[T]] =
      io.flatMap { result =>
        if(result.size > 1)
          DBIO.failed(Errors.TooManyElements)
        else
          DBIO.successful(result.headOption)
      }

    def failIfNotSingle(t: Throwable)
                       (implicit ec: ExecutionContext): DBIO[T] =
      DBIOOps(failIfMany()).failIfNone(t)

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
