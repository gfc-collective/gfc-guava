package org.gfccollective.guava

import com.google.common.base.{Predicate => GPredicate}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class PredicateTest extends AnyFunSpec with Matchers {

  describe("Predicate Functions") {
    val True: Predicate[String] = Predicate.alwaysTrue
    val False: Predicate[String] = Predicate.alwaysFalse

    describe("Operators") {
      it("should evaluate and") {
        (True && True)("") shouldBe true
        (False && True)("") shouldBe false
        (True && False)("") shouldBe false
        (False && False)("") shouldBe false
      }

      it("should evaluate or") {
        (True || True)("") shouldBe true
        (False || True)("") shouldBe true
        (True || False)("") shouldBe true
        (False || False)("") shouldBe false
      }

      it("should evaluate not") {
        !True.apply("") shouldBe false
        !False.apply("") shouldBe true
      }
    }

    describe("Companion Object") {
      it("should evaluate and") {
        Predicate.and(True)("") shouldBe true
        Predicate.and(False)("") shouldBe false
        Predicate.and(True, True)("") shouldBe true
        Predicate.and(False, True)("") shouldBe false
        Predicate.and(True, False)("") shouldBe false
        Predicate.and(False, False)("") shouldBe false
      }

      it("should evaluate or") {
        Predicate.or(True)("") shouldBe true
        Predicate.or(False)("") shouldBe false
        Predicate.or(True, True)("") shouldBe true
        Predicate.or(False, True)("") shouldBe true
        Predicate.or(True, False)("") shouldBe true
        Predicate.or(False, False)("") shouldBe false
      }

      it("should evaluate not") {
        Predicate.not(True)("") shouldBe false
        Predicate.not(False)("") shouldBe true
      }
    }
  }

  describe("Predicate conversions") {
    def scalaEval(i: Int, f: Int => Boolean): Boolean = f(i)
    def javaEval(i: Int, p: GPredicate[Int]): Boolean = p.apply(i)

    val isEven: Int => Boolean = i => i % 2 == 0

    val isOdd = new GPredicate[Int] {
      override def apply(i: Int): Boolean = i % 2 != 0
    }

    describe("direct conversions") {
      it("should convert guava->scala") {
        scalaEval(1, Predicate(isOdd)) shouldBe true
        scalaEval(0, Predicate(isOdd)) shouldBe false
      }

      it("should convert scala->") {
        javaEval(1, Predicate(isEven)) shouldBe false
        javaEval(0, Predicate(isEven)) shouldBe true
      }
    }

    describe("explicit conversions") {
      import org.gfccollective.guava.GuavaConverters._

      it("should convert guava->scala") {
        scalaEval(1, isOdd.asScala) shouldBe true
        scalaEval(0, isOdd.asScala) shouldBe false
      }

      it("should convert scala->") {
        javaEval(1, isEven.asJava) shouldBe false
        javaEval(0, isEven.asJava) shouldBe true
      }
    }


    describe("implicit conversions") {
      import org.gfccollective.guava.GuavaConversions._

      it("should convert guava->scala") {
        scalaEval(1, isOdd) shouldBe true
        scalaEval(0, isOdd) shouldBe false
      }

      it("should convert scala->") {
        javaEval(1, isEven) shouldBe false
        javaEval(0, isEven) shouldBe true
      }
    }
  }
}
