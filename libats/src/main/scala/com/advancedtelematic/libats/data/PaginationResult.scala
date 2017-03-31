package com.advancedtelematic.libats.data

final case class PaginationResult[A](total: Long, limit: Long, offset: Long, values: Seq[A]) {
  def map[B](f: A => B): PaginationResult[B] = this.copy(values = values.map(f))
}

object PaginationResult {
  import io.circe.{Encoder, Decoder}
  import io.circe.generic.semiauto._

  implicit def paginationResultEncoder[T : Encoder]: Encoder[PaginationResult[T]] = deriveEncoder
  implicit def paginationResultDecoder[T : Decoder]: Decoder[PaginationResult[T]] = deriveDecoder
}
