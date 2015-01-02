package com.gilt.gfc.guava

import com.google.common.base.{Optional, Supplier, Function => GFunction}
import org.scalatest.{Matchers, FunSuite}

/**
 * Unit tests for GuavaConverters/GuavaConversions.
 */
class GuavaConversionsTest extends FunSuite with Matchers {

  val predJava = new Predicate[String] { def apply(s: String) = s.length > 0 }
  val predScala = (s: String) => s.length > 0

  test("Converters Optional asScala") {
    import GuavaConverters._

    val optNone: Option[String] = Optional.absent[String]().asScala
    optNone should be(None)

    val optFoo: Option[String] = Optional.of("foo").asScala
    optFoo should be(Some("foo"))

    None should be(Optional.absent().asScala)
    predJava.apply("foo") should be(predJava.asScala("foo"))
  }

  test("Converters Option asJava") {
    import GuavaConverters._

    val optAbsent: Optional[String] = Option.empty[String].asJava
    optAbsent should be(Optional.absent())

    val optFoo: Optional[String] = Some("foo").asJava
    optFoo should be(Optional.of("foo"))
    None.asJava should be(Optional.absent())

    predScala("foo") should be(predScala.asJava.apply("foo"))
  }

  test("Conversions Optional asScala") {
    import GuavaConversions._
    val optNone: Option[String] = Optional.absent[String]()
    optNone should be(None)

    val optFoo: Option[String] = Optional.of("foo")
    optFoo should be(Some("foo"))

    predJava.apply("foo") should be(predJava("foo"))
  }

  test("Conversions Option asJava") {
    import GuavaConversions._

    val optAbsent: Optional[String] = Option.empty[String]
    optAbsent should be(Optional.absent())

    val optFoo: Optional[String] = Option("foo")
    optFoo should be(Optional.of("foo"))

    def predder(pred: Predicate[String], str: String) = pred.apply(str)

    predder(predScala, "foo") should be(true)
  }

  test("Conversions Function") {
    def usesGuavaFunction(input: (String, String), f: GFunction[String, String]): (String, String) = {
      (f.apply(input._1), f.apply(input._2))
    }

    def usesScalaFunction(input: (String, String), f: String => String): (String, String) = {
      (f(input._1), f(input._2))
    }

    val in0 = ("0", "1")

    // use scala anon function as guava Function
    val f0 = { s: String => "[" + s + "]" }

    // use scala partially applied method as guava Function
    def f1(v: String) = "((" + v + "))"

    // use guava Function as scala function
    val f2 = new GFunction[String, String] {
      def apply(v: String) = "<" + v + ">"
    }

    {
      import GuavaConversions._

      usesGuavaFunction(in0, f0) should be("[0]", "[1]")
      usesGuavaFunction(in0, f1 _) should be("((0))", "((1))")
      usesScalaFunction(in0, f2) should be("<0>", "<1>")
    }

    {
      import GuavaConverters._

      usesGuavaFunction(in0, f0.asJava) should be("[0]", "[1]")
      usesScalaFunction(in0, f2.asScala) should be("<0>", "<1>")
    }
  }

  test("Conversions Supplier") {
    def usesGuavaSupplier(f: Supplier[String]): (String, String) = {
      (f.get(), f.get())
    }

    def usesScalaFunction0(f: () => String): (String, String) = {
      (f(), f())
    }

    // use scala function0 as guava Supplier
    var in0 = 0
    val f0 = { () =>
      val res = "f" + in0
      in0 += 1
      res
    }

    // use scala partially applied method as guava Supplier
    var in1 = 0
    def f1 = {
      val res = "g" + in1
      in1 += 1
      res
    }

    // use guava Supplier as scala Function0
    var in2 = 0
    val f2 = new Supplier[String] {
      def get = {
        val res = "h" + in2
        in2 += 1
        res
      }
    }

    {
      import GuavaConversions._

      usesGuavaSupplier(f0) should be("f0", "f1")
      usesGuavaSupplier(f1 _) should be("g0", "g1")
      usesScalaFunction0(f2) should be("h0", "h1")

      // use scala by-name param as guava Supplier
      var in3 = 0
      val f3 = supplier {
        val res = "i" + in3
        in3 += 1
        res
      }
      usesGuavaSupplier(f3) should be("i0", "i1")
    }

    // reset counts
    in0 = 0
    in2 = 0

    {
      import GuavaConverters._

      usesGuavaSupplier(f0.asJava) should be("f0", "f1")
      usesScalaFunction0(f2.asScala) should be("h0", "h1")
    }
  }
}
