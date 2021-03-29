package com.advancedtelematic.libats.http

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directive1
import com.advancedtelematic.libats.data.DataType.Namespace
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import org.slf4j.LoggerFactory
import cats.syntax.either._
import scala.util.Try

object NamespaceDirectives {
  import akka.http.scaladsl.server.Directives._

  private lazy val config = ConfigFactory.load().getConfig("ats")

  lazy val logger = LoggerFactory.getLogger(this.getClass)

  val NAMESPACE = "x-ats-namespace"

  def nsHeader(ns: Namespace): HttpHeader = RawHeader(NAMESPACE, ns.get)

  def configNamespace(config: Config): Namespace = {
    Namespace( Try(config.getString("libats.defaultNs")).getOrElse("default"))
  }

  val fromHeader: Directive1[Option[Namespace]] =
    optionalHeaderValueByName(NAMESPACE).map(_.map(Namespace(_)))

  lazy val defaultNamespaceExtractor: Directive1[Namespace] = fromHeader.flatMap {
    case Some(ns) => provide(ns)
    case None => provide(configNamespace(ConfigFactory.load()))
  }

  def fromConfig(): Directive1[Namespace] =
    Either.catchOnly[ConfigException.Missing](ConfigFactory.load().getString("auth.protocol")) match {
      case Right("none") | Left(_) =>
        logger.info("Using namespace from default conf extractor")
        defaultNamespaceExtractor
      case Right(protocol) =>
        failWith(new IllegalArgumentException(s"auth.protocol $protocol is not supported"))
    }
}
