package com.advancedtelematic.libats.slick.db

import java.security.Security

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Source
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object SlickEncryptionKeyChange {
  case class Result(error: Int = 0, success: Int = 0)
}

class SlickEncryptionKeyChange(idColumn: String,
                               tableName: String,
                               column: String,
                               oldSalt: String,
                               oldPass: String,
                               newSalt: String,
                               newPass: String
           )
                              (implicit val db: Database, system: ActorSystem, mat: Materializer) {

  import system.dispatcher

  import SlickEncryptionKeyChange._

  private val _log = LoggerFactory.getLogger(this.getClass)

  private def updateDb(id: String, newValue: String): Future[Int] = {
    val uq = sqlu"update `#$tableName` set #$column = $newValue where #$idColumn = $id"
    db.run(uq)
  }

  private def encryptOp(oldCrypto: SlickCrypto, newCrypto: SlickCrypto)(id: String, oldValue: String): Future[Try[String]] = {
    _log.info(s"Reading old encrypted value with id $id")

    val f = for {
      value <- Future.fromTry(Try(oldCrypto.decrypt(oldValue)))
      _ <- updateDb(id, newCrypto.encrypt(value))
      _ = _log.info(s"updated encrypted value with id $id")
    } yield Success(id)

    f.recover {
      case ex =>
        _log.warn(s"Could not update encryption key for id $id", ex)
        Failure(ex)
    }
  }

  def run: Future[Result] = {
    val oldCrypto = SlickCrypto(oldSalt, oldPass)
    val newCrypto = SlickCrypto(newSalt, newPass)

    val findQuery = sql"select #$idColumn, #$column from `#$tableName`".as[(String, String)]

    val rekeyAsyncOp = encryptOp(oldCrypto, newCrypto)(_, _)

    val source = Source
      .fromPublisher(db.stream(findQuery))
      .mapAsync(3)(rekeyAsyncOp.tupled)

    val runF = source.runFold(SlickEncryptionKeyChange.Result()) {
      case (acc, Success(_)) =>
        acc.copy(success = acc.success + 1)
      case (acc, Failure(_)) =>
        acc.copy(error = acc.error + 1)
    }

    runF.map { res =>
      if(res.error > 0) {
        _log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        _log.error(s"There were ${res.error} errors")
        _log.error("This might mean records are encrypted with different keys now")
        _log.error(s" Database needs to be checked for consistency or restored from backups")
        _log.error(s"${res.success} records were updated successfully")
        _log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        res
      } else {
        _log.info(s"Finished. Updated ${res.success} records")
        res
      }
    }
  }
}


object SlickEncryptionKeyChangeApp extends DatabaseConfig {
  implicit lazy val system = ActorSystem("SlickEncryptionKeyChangeApp")

  implicit lazy val mat = ActorMaterializer()

  implicit val _db = db

  Security.addProvider(new BouncyCastleProvider)

  def main(args: Array[String]): Unit = {
    args match {
      case Array(idColumn,
      tableName,
      column,
      oldSalt,
      oldPass,
      newSalt,
      newPass) =>
        val f = new SlickEncryptionKeyChange(idColumn, tableName, column,
          oldSalt, oldPass, newSalt, newPass).run

        val res = Await.result(f, Duration.Inf)

        system.terminate()

        if(res.error > 0)
          sys.exit(-2)

      case _ =>
        println("usage: SlickEncryptionKeyChangeApp idColumn tableName column oldSalt oldPass newSalt newPass")
        sys.exit(-1)
    }
  }
}
