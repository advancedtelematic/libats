package com.advancedtelematic.libats.data

final case class PaginationResult[A](values: Seq[A], total: Long, offset: Long, limit: Long) {
  def map[B](f: A => B): PaginationResult[B] = this.copy(values = values.map(f))
}

object PaginationResult {
  import io.circe.{Encoder, Decoder}
  import io.circe.generic.semiauto._

  implicit def paginationResultEncoder[T : Encoder]: Encoder[PaginationResult[T]] = deriveEncoder
  implicit def paginationResultDecoder[T : Decoder]: Decoder[PaginationResult[T]] = deriveDecoder
}
