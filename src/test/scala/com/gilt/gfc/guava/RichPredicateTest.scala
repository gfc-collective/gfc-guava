package com.gilt.gfc.guava

import com.google.common.base.{ Predicate, Predicates }
import org.scalatest.{FunSuite, Matchers}

class RichPredicateTest extends FunSuite with Matchers {

  val True: Predicate[String] = Predicates.alwaysTrue[String]
  val False: Predicate[String] = Predicates.alwaysFalse[String]

  test("RichPredicate") {
    import GuavaConversions._

    (False && True) should be(andp(False, True))
    (True && False) should (not be andp(False, True))

    (False || True) should be(orp(False, True))
    (True || False) should (not be orp(False, True))

    (!True) should be(notp(True))
    (!False) should (not be notp(True))

    (False && True || False) should be(orp(andp(False, True), False))
    (False && True || False) should (not be andp(False, orp(True, False)))

    (False && (True || False)) should be(andp(False, orp(True, False)))
    (False && (True || False)) should (not be orp(andp(False, True), False))

    (True && (False || False) || !False) should be(orp(andp(True, orp(False, False)), notp(False)))
  }

  def andp[T](predicates: Predicate[T]*) = Predicates.and[T](predicates: _*)

  def orp[T](predicates: Predicate[T]*) = Predicates.or[T](predicates: _*)

  def notp[T](predicate: Predicate[T]) = Predicates.not[T](predicate)
}
