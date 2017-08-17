package com.advancedtelematic.libats.slick.db

import org.flywaydb.core.api.migration.MigrationChecksumProvider
import org.flywaydb.core.api.migration.jdbc.JdbcMigration

import scala.concurrent.{Await, Future}
import java.sql.Connection

import slick.jdbc.JdbcBackend.{BaseSession, DatabaseDef}
import slick.jdbc.{JdbcBackend, JdbcDataSource}
import slick.util.AsyncExecutor

import scala.concurrent.duration.Duration


import slick.jdbc.MySQLProfile.api._

object MigrationDatabase {
  def apply(conn: Connection): MigrationDatabase = new MigrationDatabase(conn)

  private class MigrationJdbcDataSource(conn: Connection) extends JdbcDataSource {
    override def createConnection() = conn

    override def close() = ()

    override val maxConnections = Some(1)
  }

  protected class MigrationSession(database: DatabaseDef) extends BaseSession(database) {
    override def close() = ()
  }

  protected class MigrationDatabase(conn: Connection) extends JdbcBackend.DatabaseDef(new MigrationJdbcDataSource(conn), AsyncExecutor("MigrationUmanagedDatabase-Executor", 1, -1)) {
    override def createSession() = new MigrationSession(this)
  }
}

trait AppMigration extends JdbcMigration with MigrationChecksumProvider {

  override def getChecksum: Integer = 0

  def migrate(implicit db: Database): Future[Unit]

  override def migrate(connection: Connection): Unit = {
    val f = migrate(MigrationDatabase(connection))
    Await.result(f, Duration.Inf)
  }
}
