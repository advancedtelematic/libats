package com.advancedtelematic.libats.http

import akka.http.scaladsl.model.StatusCodes
import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.LoggerFactory

object LoggingResource {
  import akka.http.scaladsl.server.Directives._

  private val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

  private def loggers() = {
    import scala.collection.JavaConverters._
    loggerContext.getLoggerList.asScala
  }

  val route =
    pathPrefix("loggers" ) {
      pathEnd {
        get {
          complete( StatusCodes.OK -> loggers().map(x => s"${x.getName} => ${x.getEffectiveLevel}").mkString("\n"))
        }
      } ~
        pathPrefix(Segment) { loggerName =>
          pathEnd {
            get{
              loggers().find(_.getName == loggerName) match {
                case Some(logger) => complete(StatusCodes.OK -> s"${logger.getName}\t${logger.getEffectiveLevel}")
                case None => complete(StatusCodes.NotFound)
              }
            } ~
              put{
                (formField("level") | parameter("level")) { lvl =>
                  val logger = loggerContext.getLogger(loggerName)
                  logger.setLevel(Level.valueOf(lvl))
                  complete(StatusCodes.OK -> s"${logger.getName}\t${logger.getEffectiveLevel}")
                }
              }
          }
        }
    }
}

