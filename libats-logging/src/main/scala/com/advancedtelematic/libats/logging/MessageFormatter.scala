package com.advancedtelematic.libats.logging

sealed abstract class MessageFormatter {

  def serviceName: String

  def getMessage(remoteAddr: String,
                 requestMethod: String,
                 requestUri: String,
                 responseContentLength: Int,
                 responseStatusCode: Int,
                 replyTimeMs: Long): String
}

class JsonMessageFormatter(val serviceName: String) extends MessageFormatter {

  def getMessage(remoteAddr: String,
                 requestMethod: String,
                 requestUri: String,
                 responseContentLength: Int,
                 responseStatusCode: Int,
                 replyTimeMs: Long): String = {
    val data = List(
      "remote_addr" -> remoteAddr,
      "method" -> requestMethod,
      "path" -> requestUri,
      "service_name" -> serviceName,
      "stime" -> replyTimeMs.toString,
      "status" -> responseStatusCode.toString
    ) ++ (if (responseContentLength > 0) List("content_ln" -> responseContentLength.toString) else List.empty)

    val builder = data.zipWithIndex.foldLeft(new StringBuilder("{")) { case (sb, ((k, v), idx)) =>
      sb.append("\"").append(k).append("\":").append("\"").append(v).append("\"")

      if (idx < data.length - 1)
        sb.append(",")
      else
        sb
    }

    builder.append("}")
    builder.toString
  }
}

class RawMessageFormatter(val serviceName: String) extends MessageFormatter {

  def getMessage(remoteAddr: String,
                 requestMethod: String,
                 requestUri: String,
                 responseContentLength: Int,
                 responseStatusCode: Int,
                 replyTimeMs: Long): String = {
    val builder = new StringBuilder
    builder.append("method=").append(requestMethod)
    builder.append(" path=").append(requestUri)
    builder.append(" service_name=").append(serviceName)
    builder.append(" stime=").append(replyTimeMs)
    builder.append(" status=").append(responseStatusCode.toString)
    builder.append(" remote_addr=").append(remoteAddr)
    if (responseContentLength > 0) builder.append(" content_ln=").append(responseContentLength)
    builder.toString
  }
}
