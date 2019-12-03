package com.advancedtelematic.libats.logging

import java.time.Instant

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.pattern.{TargetLengthBasedClassNameAbbreviator, ThrowableProxyConverter}
import ch.qos.logback.classic.spi.ILoggingEvent
import io.circe.syntax._
import io.circe.{Encoder, Json}

import scala.collection.JavaConverters._

class JsonEncoder extends ch.qos.logback.core.encoder.EncoderBase[ILoggingEvent] {
  private var includeContext = false
  private var includeThread = false
  private var includeMdc = false
  private var includeHttpQuery = false
  private var prettyPrint = false
  private var loggerLength = 36
  private var msgIsJson = false

  private val throwableProxyConverter = new ThrowableProxyConverter
  private val abbreviator = new TargetLengthBasedClassNameAbbreviator(loggerLength)

  implicit private val levelEncoder: Encoder[Level] = Encoder.encodeString.contramap(_.toString)

  def setLoggerLength(value: Int): Unit = loggerLength = 36

  def setIncludeContext(value: Boolean): Unit = includeContext = value

  def setIncludeThread(value: Boolean): Unit = includeThread = value

  def setPrettyPrint(value: Boolean): Unit = prettyPrint = value

  def setIncludeMdc(value: Boolean): Unit = includeMdc = value

  def setIncludeQuery(value: Boolean): Unit = includeHttpQuery = value

  def setMsgIsJson(value: Boolean): Unit = msgIsJson = value

  override def start(): Unit = {
    throwableProxyConverter.start()
    super.start()
  }

  override def stop(): Unit = {
    throwableProxyConverter.stop()
    super.stop()
  }

  private def formatMsgJson(msg: String): Json = {
    if(msgIsJson)
      io.circe.jawn.parse(msg).toOption.getOrElse(msg.asJson)
    else
      msg.asJson
  }

  override def encode(event: ILoggingEvent): Array[Byte] = {
    val mdc = event.getMDCPropertyMap.asScala.mapValues(_.asJson)

    val map = Map[String, Json](
      "at" -> Instant.ofEpochMilli(event.getTimeStamp).asJson,
      "level" -> event.getLevel.asJson,
      "logger" -> abbreviator.abbreviate(event.getLoggerName).asJson,
      "msg" -> formatMsgJson(event.getFormattedMessage)
    )
      .withValue(includeContext, "ctx" -> event.getLoggerContextVO.toString.asJson)
      .withValue(includeThread, "thread" -> event.getThreadName.asJson)
      .withValue(includeMdc, "mdc" -> mdc.asJson)
      .withValue("throwable" -> encodeThrowable(event))
      .maybeWithValue("req_namespace", mdc.get("req_namespace"))
      .maybeWithValue("http_status", mdc.get("http_status"))
      .maybeWithValue("http_method", mdc.get("http_method"))
      .maybeWithValue("http_service_name", mdc.get("http_service_name"))
      .maybeWithValue("http_stime", mdc.get("http_stime"))
      .maybeWithValue("http_path", mdc.get("http_path"))
      .maybeWithValue("http_query", mdc.get("http_query").filter(_ => includeHttpQuery))

    val str = if(prettyPrint) map.asJson.spaces2 else map.asJson.noSpaces

    (str + "\n").getBytes
  }

  protected def encodeThrowable(value: ILoggingEvent): Json = {
    val maybeEx = Option(throwableProxyConverter.convert(value)).filter(_ != "")
    maybeEx.map(_.asJson).getOrElse(Json.Null)
  }

  override def headerBytes(): Array[Byte] = null

  override def footerBytes(): Array[Byte] = null

  implicit private class MapJsonOps(map: Map[String, Json]) {
    def maybeWithValue(value: => (String, Option[Json])): Map[String, Json] =
      value match {
        case (key, Some(v)) =>
          map + (key -> v)
        case _ =>
          map
      }

    def withValue(enabled: Boolean, value: => (String, Json)): Map[String, Json] =
      if(enabled)
        map + value
      else
        map

    def withValue(value: => (String, Json)): Map[String, Json] = value match {
      case (_, v) if v != Json.Null && v != Json.obj() => map + value
      case _ => map
    }
  }
}

