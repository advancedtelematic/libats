package com.advancedtelematic.libats.http

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import com.advancedtelematic.libats.data.{ValidatedString, ValidatedStringConstructor, ValidationError}
import com.advancedtelematic.libats.http.ValidatedStringMarshallingSupport._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, Matchers, PropSpec}

class ValidatedStringMarshallingSupportSpec extends PropSpec with Matchers with EitherValues with ScalaFutures {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher

  property("should unmarshall a ValidatedString") {
    val unmarshaller = implicitly[Unmarshaller[String, ValidatedString]]
    val res = unmarshaller("abc")
    res.futureValue shouldBe ValidatedStringConstructor[ValidatedString]("abc").right.value
  }

  property("should fail to unmarshall an invalid ValidatedString") {
    val unmarshaller = implicitly[Unmarshaller[String, ValidatedString]]
    unmarshaller("abXba").failed.futureValue shouldBe a [ValidationError]
  }

}
