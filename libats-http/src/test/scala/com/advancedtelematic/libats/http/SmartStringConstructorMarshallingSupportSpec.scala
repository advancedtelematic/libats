package com.advancedtelematic.libats.http

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import com.advancedtelematic.libats.data.{SmartStringConstructor, ValidatedString, ValidationError}
import com.advancedtelematic.libats.http.SmartConstructorMarshallingSupport._
import org.scalatest.{EitherValues, Matchers, PropSpec}
import org.scalatest.concurrent.ScalaFutures

class SmartStringConstructorMarshallingSupportSpec extends PropSpec with Matchers with ScalaFutures with EitherValues {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher

  property("should unmarshall a SmartConstructor") {
    val unmarshaller = implicitly[Unmarshaller[String, ValidatedString]]
    val res = unmarshaller.apply("abc")
    res.futureValue shouldBe SmartStringConstructor[ValidatedString]("abc").right.value
  }

  property("should fail to unmarshall an invalid SmartConstructor") {
    val unmarshaller = implicitly[Unmarshaller[String, ValidatedString]]
    unmarshaller.apply("abXba").failed.futureValue shouldBe a [ValidationError]
  }

}
