package com.advancedtelematic.libats.http

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import cats.syntax.either._
import cats.syntax.option._
import com.advancedtelematic.libats.data.DataType.Namespace
import com.advancedtelematic.libats.data.ErrorRepresentation
import com.advancedtelematic.libats.http.Errors.RemoteServiceError
import com.advancedtelematic.libats.http.ServiceHttpClient.{ServiceHttpFullResponse, ServiceHttpFullResponseEither}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.{Encoder, Json}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

trait ServiceHttpClientSupport {
  def defaultHttpClient(implicit system: ActorSystem, mat: Materializer): HttpRequest => Future[HttpResponse] = {
    val _http = Http()
    req => _http.singleRequest(req)
  }
}

object ServiceHttpClient {
  type ServiceHttpFullResponseEither[T] = Either[RemoteServiceError, ServiceHttpFullResponse[T]]

  case class ServiceHttpFullResponse[T](httpResponse: HttpResponse, unmarshalled: T)

  implicit class ServiceHttpFullResponseFutureOps[R](value: Future[Either[RemoteServiceError, R]]) {
    def ok(implicit ec: ExecutionContext): Future[R] =
      value.flatMap {
        case Left(err) => FastFuture.failed(err)
        case Right(v) => FastFuture.successful(v)
      }

    def handleErrors(pf: PartialFunction[RemoteServiceError, Future[R]])(implicit ec: ExecutionContext): Future[R] = {
      value.flatMap {
        case Left(err) if pf.isDefinedAt(err) => pf.apply(err)
        case Left(err) => FastFuture.failed(err)
        case Right(v) => FastFuture.successful(v)
      }
    }
  }
}

abstract class ServiceHttpClient(_httpClient: HttpRequest => Future[HttpResponse])
                                (implicit system: ActorSystem, mat: Materializer) {
  import Errors.RemoteServiceError
  import io.circe.syntax._
  import system.dispatcher

  private val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val unitFromEntityUnmarshaller: FromEntityUnmarshaller[Unit] = Unmarshaller.strict(_.discardBytes())

  protected def httpClient = _httpClient

  protected def execJsonHttp[Res : ClassTag : FromEntityUnmarshaller, Req : Encoder]
  (request: HttpRequest, entity: Req): Future[Either[RemoteServiceError, Res]] = {
    val httpEntity = HttpEntity(ContentTypes.`application/json`, entity.asJson.noSpaces)
    val req = request.withEntity(httpEntity)
    execHttpUnmarshalled(req)
  }

  private def tryErrorParsing(response: HttpResponse)(implicit um: FromEntityUnmarshaller[ErrorRepresentation]): Future[RemoteServiceError] = {
    um(response.entity).map { rawError =>
      RemoteServiceError(s"${rawError.description}", response.status, rawError.cause.getOrElse(Json.Null),
        rawError.code, rawError.some, rawError.errorId.getOrElse(UUID.randomUUID()))
    }.recoverWith { case _ =>
      Unmarshaller.stringUnmarshaller(response.entity).map(msg => RemoteServiceError(msg, response.status))
    }.recover { case _ =>
      RemoteServiceError(s"Unknown error: $response", response.status)
    }
  }

  protected def execHttpUnmarshalled[T : ClassTag](request: HttpRequest)
                                                  (implicit um: FromEntityUnmarshaller[T]): Future[Either[RemoteServiceError, T]] =
    execHttpFull(request).map { _.map(_.unmarshalled) }


  protected def execHttpFull[T : ClassTag](request: HttpRequest)
                                          (implicit um: FromEntityUnmarshaller[T]): Future[Either[RemoteServiceError, ServiceHttpFullResponse[T]]] =
    httpClient(request).flatMap {
      case r @ HttpResponse(status, _, _, _) if status.isSuccess() =>
        um(r.entity).map(e => ServiceHttpFullResponse(r, e).asRight)
      case r =>
        tryErrorParsing(r).flatMap { error =>
          log.debug(s"request failed: $request")
          val e = error.copy(msg = s"${this.getClass.getSimpleName}|Unexpected response from remote server at ${request.uri}|${request.method.value}|${r.status.intValue()}|${error.msg}")

          Try(r.discardEntityBytes())

          FastFuture.successful(e.asLeft)
        }
    }

  protected def execHttpUnmarshalledWithNamespace[T : ClassTag : FromEntityUnmarshaller](namespace: Namespace, request: HttpRequest): Future[Either[RemoteServiceError, T]] =
    execHttpFullWithNamespace[T](namespace, request).map(_.map(_.unmarshalled))

  protected def execHttpFullWithNamespace[T : ClassTag : FromEntityUnmarshaller](namespace: Namespace, request: HttpRequest): Future[ServiceHttpFullResponseEither[T]] = {
    val req = request.addHeader(RawHeader("x-ats-namespace", namespace.get))
    execHttpFull[T](req)
  }
}
