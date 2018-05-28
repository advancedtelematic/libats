package com.advancedtelematic.libats.slick.db

import com.advancedtelematic.libats.http.BootApp

import scala.util.{Failure, Success, Try}

trait SlickEncryptionConfig {
  self: BootApp =>

  Try(SlickCrypto.configSlickCrypto) match {
    case Success(_) =>
      log.info("SlickCrypto initialized successfully")
    case Failure(ex) =>
      log.error("Could not initialize SlickCrypto", ex)
      throw ex
  }
}
