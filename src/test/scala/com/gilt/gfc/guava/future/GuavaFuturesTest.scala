package com.gilt.gfc.guava.future

import java.util.concurrent.{Executors, ExecutionException}
import scala.collection.JavaConverters._
import com.google.common.base.{Optional, Predicate, Predicates}
import com.google.common.util.concurrent.ListenableFuture
import org.scalatest.{Matchers, FunSuite}

/**
 * Tests the basics of the Future monad wrapper for Guava futures.
 *
 * @author Eric Bowman
 * @since 11/22/12 1:50 PM
 */
class GuavaFuturesTest extends FunSuite with Matchers {

  // pull in the implicit magic so Guava's ListenableFuture presents a monadic interface

  import com.gilt.gfc.guava.future.GuavaFutures._

  // some test fixtures
  val future7 = ListenableFutureNow(7)

  def service1(x: Int): ListenableFuture[Int] = ListenableFutureNow(x)
  def service2(y: Int): ListenableFuture[Int] = ListenableFutureNow(y + 1)
  def service3(z: Int, delay: Long): ListenableFuture[Int] = GuavaFutures.future {
    Thread.sleep(delay)
    z
  }

  def MustBePositive = new Predicate[Int]() {
    override def apply(t: Int): Boolean = t >= 0
  }

  implicit val executor = Executors.newCachedThreadPool()

  def failedServiceCall: ListenableFuture[Int] = GuavaFutures.future {
    Thread.sleep(100L)
    sys.error("Failed Service Call")
  }

  test("basic map") {
    // mapping lets you chain a computation that should be done when a future completes, and wraps
    // that computation in a future
    val future8 = future7.map(_ + 1)
    future8.get should equal(8)
    future7.map(_ + 1).map(_ + 1).get should equal(9)
  }

  test("foreach") {
    // foreach lets you pass in work that should be done when the future completes
    future7.foreach(x => x should equal(7))
  }

  test("flatmap") {

    // flatMap on a future is handy when you need to call a second service passing in the result of
    // a call to an earlier service
    val result = for {
      x <- service1(1) // returns a future of 1
      y <- service2(x) // returns a future of x + 1
    } yield y

    result.get() should equal(2) // result is a future of 1+1
  }

  test("filter success") {
    // like the flatmap example, except this only keeps going if the filter passes.
    // if the filter doesn't pass, future evaluations don't occur, and the final future blows exception
    val result = for {
      x <- service1(1)
      y <- service2(x) if y == 2
    } yield y

    result.get() should equal(2)
  }

  test("filter failure") {

    // filtering causes an exception downstream if the filter didn't match
    val result = for {
      x <- service1(1)
      y <- service2(x) if y == 1
    } yield y

    // when the filter fails, you end up with a future that will blow an execution exception
    // upon access
    evaluating {
      result.get()
    } should produce[ExecutionException]

    // The execution exception should wrap a MatchError that indicates what failed
    try {
      result.get()
    } catch {
      case ex if !ex.getCause.isInstanceOf[MatchError] => fail("Wrapped exception should be MatchError")
      case ex: Throwable => ex.getCause.toString.contains("MatchError: 2 (of class java.lang.Integer)") should be(true)
    }
  }

  test("subsequent evaluation stops after filter fails") {
    // When the filter fails, the subsequent clauses in the for comprehension should not execute
    var didExecute = false
    def service3(x: Int): ListenableFuture[Int] = ListenableFutureNow {
      didExecute = true; x
    }
    val result = for {
      x <- service1(1)
      y <- service2(x) if y == 1
      z <- service3(y)
    } yield z

    evaluating {
      result.get()
    } should produce[ExecutionException]

    didExecute should equal(false)
  }

  test("withEitherFallback success") {
    service1(9).withEitherFallback.get match {
      case Right(9) => /** good, expected */
      case Right(wtf) => fail("Expected test value, got [%s]".format(wtf))
      case Left(_) => fail("Expected successful call")
    }
  }

  test("withEitherFallback failure") {
    failedServiceCall.withEitherFallback.get match {
      case Right(_) => fail("Expected call to fail")
      case Left(t) => t.getMessage should equal("Failed Service Call")
    }
  }

  test("withOptionFallback success") {
    service1(9).withOptionFallback { _: Throwable =>
      fail("Don't expect error callback to be executed during successful call")
    }.get match {
      case Some(9) => /** good, expected */
      case Some(wtf) => fail("Expected test value, got [%s]".format(wtf))
      case None => fail("Expected successful call")
    }
  }

  test("withOptionFallback failure") {
    var errMessage = "-"
    failedServiceCall.withOptionFallback { err: Throwable =>
      errMessage = err.getMessage
    }.get match {
      case Some(_) => fail("Expected call to fail")
      case None => errMessage should equal("Failed Service Call")
    }
  }

  test("withDefault success") {
    service1(9).withDefault(0, { _: Throwable =>
      fail("Don't expect error callback to be executed during successful call")
    }).get match {
      case 9 => /** good, expected */
      case wtf => fail("Expected test value, got [%s]".format(wtf))
    }
  }

  test("withDefault failure") {
    var errMessage = "-"
    failedServiceCall.withDefault(0, { err: Throwable =>
      errMessage = err.getMessage
    }).get match {
      case 0 =>
        /** good, expected */
        errMessage should equal("Failed Service Call")
      case _ => fail("Expected call to fail")
    }
  }

  test("firstCompletedOf must return back a deferred None on empty list of futures") {
    GuavaFutures.firstCompletedOf[String](List.empty[ListenableFuture[String]].asJava).get should equal(Optional.absent[String])
  }

  test("find must return back a deferred None on empty list of futures") {
    GuavaFutures.find[String](List.empty[ListenableFuture[String]].asJava, Predicates.alwaysTrue[String]).get should equal(Optional.absent[String])
  }

  test("firstCompletedOf must return the only future passed in") {
    val f1 = service3(10, 100)
    GuavaFutures.firstCompletedOf(List(f1).asJava).get should equal (Optional.of(10))
  }

  test("find must return the only matching future passed in") {
    val f1 = service3(10, 100)
    GuavaFutures.find(List(f1).asJava, MustBePositive).get should equal (Optional.of(10))
  }

  test("find must return None if the only future passed in is not matching") {
    val f1 = service3(-10, 100)
    GuavaFutures.find(List(f1).asJava, MustBePositive).get should equal (Optional.absent[Int])
  }

  test("firstCompletedOf must return None if the only future passed is failing") {
    val f1 = failedServiceCall
    GuavaFutures.firstCompletedOf(List(f1).asJava).get should equal (Optional.absent[Int])
  }

  test("find must return None if the only future passed is failing") {
    val f1 = failedServiceCall
    GuavaFutures.find(List(f1).asJava, MustBePositive).get should equal (Optional.absent[Int])
  }

  test("firstCompletedOf must return the first future that completes successfully and discard the other one") {
    val f1 = service3(10, 100)
    val f2 = service3(20, 1000)
    val f3 = failedServiceCall
    val f4 = service3(30, 3000)
    val f5 = service3(40, 4000)
    val f6 = failedServiceCall

    GuavaFutures.firstCompletedOf(List(f1, f2, f3, f4, f5, f6).asJava).get should equal (Optional.of(10))

    val now = System.currentTimeMillis
    val f10 = failedServiceCall
    val f11 = service3(40, 4000)
    val f12 = failedServiceCall
    val f13 = failedServiceCall
    val f14 = service3(30, 3000)

    GuavaFutures.firstCompletedOf(List(f10, f11, f12, f13, f14).asJava).get should equal (Optional.of(30))
    val elapsed: Long = (System.currentTimeMillis - now)
    (elapsed < 3900) should be (true)
  }

  test("find must return the first future that completes successfully and matches the predicate while discarding the others") {
    val f1 = service3(-10, 100)
    val f2 = service3(-20, 1000)
    val f3 = service3(30, 3000)
    val f4 = service3(-40, 4000)

    GuavaFutures.find(List(f1, f2, f3, f4).asJava, MustBePositive).get should equal (Optional.of(30))
  }

  test("find must return None if all the futures are not matching the predicate") {
    val f1 = service3(-10, 100)
    val f2 = service3(-20, 1000)
    val f3 = service3(-30, 3000)
    val f4 = service3(-40, 4000)

    GuavaFutures.find(List(f1, f2, f3, f4).asJava, MustBePositive).get should equal (Optional.absent[Int])
  }

  test("find must return None if all the futures are failing except one but it still does not matches the predicate") {
    val f1 = failedServiceCall
    val f2 = failedServiceCall
    val f3 = service3(-30, 100)
    val f4 = failedServiceCall

    GuavaFutures.find(List(f1, f2, f3, f4).asJava, MustBePositive).get should equal (Optional.absent[Int])
  }

  test("find must return the fastest future that comes back and matches the predicate") {
    val now = System.currentTimeMillis
    val f1 = service3(10, 100)
    val f2 = service3(20, 1000)
    val f3 = service3(30, 3000)
    val f4 = service3(40, 4000)

    GuavaFutures.find(List(f1, f2, f3, f4).asJava, MustBePositive).get should equal (Optional.of(10))
    val elapsed = (System.currentTimeMillis - now)
    (elapsed < 900) should be (true)
  }

  test("firstCompletedOf must return None if all the passed futures are failing") {
    val f1 = failedServiceCall
    val f2 = failedServiceCall

    GuavaFutures.firstCompletedOf(List(f1, f2).asJava).get should equal (Optional.absent[Int])
  }
}
