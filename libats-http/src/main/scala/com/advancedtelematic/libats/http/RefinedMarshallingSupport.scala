package com.advancedtelematic.libats.http
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import eu.timepit.refined.api.{Refined, Validate}
import com.advancedtelematic.libats.data.RefinedUtils._
import scala.concurrent.ExecutionContext

object RefinedMarshallingSupport {

  implicit def refinedUnmarshaller[P]
  (implicit p: Validate.Plain[String, P]): FromStringUnmarshaller[Refined[String, P]] =
    Unmarshaller.strict[String, Refined[String, P]] { _.refineTry[P].get }

  implicit def refinedFromRequestUnmarshaller[T, P](implicit um: FromEntityUnmarshaller[T],
                                                    p: Validate.Plain[T, P],
                                                    ec: ExecutionContext,
                                                    mat: Materializer): FromRequestUnmarshaller[Refined[T, P]]
  = Unmarshaller { implicit ec => request =>
    um(request.entity).flatMap { (t: T) => FastFuture(t.refineTry[P]) }
  }
}
