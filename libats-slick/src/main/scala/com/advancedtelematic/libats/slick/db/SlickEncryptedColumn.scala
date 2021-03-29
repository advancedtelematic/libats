package com.advancedtelematic.libats.slick.db

import java.util.Base64

import cats.data.{Validated, ValidatedNel}
import javax.crypto.Cipher
import javax.crypto.spec.PBEParameterSpec
import io.circe.{Decoder, Encoder}

import scala.reflect.ClassTag
import slick.jdbc.MySQLProfile.api._

import com.typesafe.config.ConfigFactory
import io.circe.parser
import io.circe.syntax._
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.bouncycastle.jce.provider.BouncyCastleProvider


import cats.implicits._


protected [db] class SlickCrypto(salt: Array[Byte], password: String) {
  private lazy val pbeParameterSpec = new PBEParameterSpec(salt, 1000)
  private val BC = BouncyCastleProvider.PROVIDER_NAME
  private lazy val CIPHER_ALGORITHM = "AES/GCM/NoPadding"

  private lazy val pbeKey = {
    val pbeKeySpec = new PBEKeySpec(password.toCharArray)
    val keyFac = SecretKeyFactory.getInstance("PBEWithSHA256And256BitAES-CBC-BC", BC)
    keyFac.generateSecret(pbeKeySpec)
  }

  def decrypt(str: String): String = {
    val bytes = Base64.getDecoder.decode(str)

    val encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, BC)
    encryptionCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParameterSpec)

    val plainTextBytes = encryptionCipher.doFinal(bytes)

    new String(plainTextBytes)
  }

  def encrypt(plainText: String): String = {
    val encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, BC)
    encryptionCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParameterSpec)

    val ciphered = encryptionCipher.doFinal(plainText.getBytes())

    Base64.getEncoder.encodeToString(ciphered)
  }
}

protected [db] object SlickCrypto {
  import cats.syntax.either._

  private lazy val _config = ConfigFactory.load()

  private lazy val dbEncryptionSaltBase64String = _config.getString("ats.database.encryption.salt")

  private lazy val dbEncryptionPassword = _config.getString("ats.database.encryption.password")

  lazy val configSlickCrypto = SlickCrypto(dbEncryptionSaltBase64String, dbEncryptionPassword)

  def apply(saltBase64String: String, password: String): SlickCrypto = {
    val saltBytes = Base64.getDecoder.decode(saltBase64String.getBytes)

    val saltV = Validated.condNel(saltBytes.length >= 8, saltBytes, "SlickCrypto: Salt needs to be base64 encoded and 8 bytes or longer")
    val passV = Validated.condNel(password.length >= 64, password, "SlickCrypto: password needs to be 64 chars or longer")

    val slickCryptoV: ValidatedNel[String, SlickCrypto] = (saltV, passV).mapN { (saltbytes, password) =>
      new SlickCrypto(saltBytes, password)
    }

    slickCryptoV.toEither.leftMap { errors =>
      new IllegalArgumentException(errors.toList.mkString(", "))
    }.valueOr(throw _)
  }
}

object SlickEncryptedColumn {
  import cats.syntax.either._
  import SlickCrypto.configSlickCrypto

  case class EncryptedColumn[T](value: T) extends AnyVal

  def encryptedColumnJsonMapper[T : ClassTag : Encoder : Decoder]: BaseColumnType[EncryptedColumn[T]] =
    MappedColumnType.base[EncryptedColumn[T], String](
      encryptedColumn => configSlickCrypto.encrypt(encryptedColumn.value.asJson.noSpaces)
      ,
      str => {
        val parsedJson = parser.parse(configSlickCrypto.decrypt(str)).flatMap(_.as[T]).valueOr(throw _)
        EncryptedColumn(parsedJson)
      }
    )

  implicit class EncryptedColumnRepToRepOps[T : ClassTag](value: Rep[EncryptedColumn[T]])
                                                         (implicit shape: Shape[_ <: FlatShapeLevel, Rep[EncryptedColumn[T]], EncryptedColumn[T], _]) {
    def decrypted = anyToShapedValue(value) <> (ec => ec.value, (v: T) => Some(EncryptedColumn(v)))
  }
}
