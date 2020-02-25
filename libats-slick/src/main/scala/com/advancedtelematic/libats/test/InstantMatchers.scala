package com.advancedtelematic.libats.test

import java.time.Instant

import org.scalatest.matchers.{BeMatcher, MatchResult}

trait InstantMatchers {
  def after(other: Instant): BeMatcher[Instant] = (me: Instant) => MatchResult(me.isAfter(other),
    me + " was not after " + other,
    me + " was after " + other)

  def before(other: Instant): BeMatcher[Instant] = (me: Instant) => MatchResult(me.isBefore(other),
    me + " was not before " + other,
    me + " was before " + other)
}

object InstantMatchers extends InstantMatchers