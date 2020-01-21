package org.gfccollective.guava.future

import java.util.concurrent.Executors
import org.gfccollective.guava.Predicate
import com.google.common.util.concurrent.{Futures, ListenableFuture}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Tests the basics of the Future monad wrapper for Guava futures.
 *
 * @author Eric Bowman
 * @since 11/22/12 1:50 PM
 */
class GuavaFuturesTest extends AnyFunSuite with Matchers {

  // pull in the implicit magic so Guava's ListenableFuture presents a monadic interface

  implicit val executor = Executors.newCachedThreadPool()

  // some test fixtures
  val future7 = Futures.immediateFuture(7)

  def service1(x: Int): ListenableFuture[Int] = Futures.immediateFuture(x)
  def service2(y: Int): ListenableFuture[Int] = Futures.immediateFuture(y + 1)
  def service3(z: Int, delay: Long): ListenableFuture[Int] = GuavaFutures.future {
    Thread.sleep(delay)
    z
  }

  def MustBePositive = new Predicate[Int]() {
    override def apply(t: Int): Boolean = t >= 0
  }

  def failedServiceCall(delay: Long = 0L, exc: => Nothing = sys.error("Failed Service Call")): ListenableFuture[Int] = GuavaFutures.future {
    Thread.sleep(delay)
    exc
  }


  test("firstCompletedOf must return back a deferred None on empty list of futures") {
    GuavaFutures.firstCompletedOf[String](List.empty[ListenableFuture[String]]).get should equal(None)
  }

  test("find must return back a deferred None on empty list of futures") {
    GuavaFutures.find[String](List.empty[ListenableFuture[String]])(Predicate.alwaysTrue[String]).get should equal(None)
  }

  test("firstCompletedOf must return the only future passed in") {
    val f1 = service3(10, 100)
    GuavaFutures.firstCompletedOf(List(f1)).get should equal (Some(10))
  }

  test("find must return the only matching future passed in") {
    val f1 = service3(10, 100)
    GuavaFutures.find(List(f1))(MustBePositive).get should equal (Some(10))
  }

  test("find must return None if the only future passed in is not matching") {
    val f1 = service3(-10, 100)
    GuavaFutures.find(List(f1))(MustBePositive).get should equal (None)
  }

  test("firstCompletedOf must return None if the only future passed is failing") {
    val f1 = failedServiceCall(100L)
    GuavaFutures.firstCompletedOf(List(f1)).get should equal (None)
  }

  test("find must return None if the only future passed is failing") {
    val f1 = failedServiceCall()
    GuavaFutures.find(List(f1))(MustBePositive).get should equal (None)
  }

  test("firstCompletedOf must return the first future that completes successfully and discard the other one") {
    val f1 = service3(10, 100)
    val f2 = service3(20, 1000)
    val f3 = failedServiceCall(100L)
    val f4 = service3(30, 3000)
    val f5 = service3(40, 4000)
    val f6 = failedServiceCall(100L)

    GuavaFutures.firstCompletedOf(List(f1, f2, f3, f4, f5, f6)).get should equal (Some(10))

    val now = System.currentTimeMillis
    val f10 = failedServiceCall(100L)
    val f11 = service3(40, 4000)
    val f12 = failedServiceCall(100L)
    val f13 = failedServiceCall(100L)
    val f14 = service3(30, 3000)

    GuavaFutures.firstCompletedOf(List(f10, f11, f12, f13, f14)).get should equal (Some(30))
    val elapsed: Long = (System.currentTimeMillis - now)
    (elapsed < 3900) should be (true)
  }

  test("find must return the first future that completes successfully and matches the predicate while discarding the others") {
    val f1 = service3(-10, 100)
    val f2 = service3(-20, 1000)
    val f3 = service3(30, 3000)
    val f4 = service3(-40, 4000)

    GuavaFutures.find(List(f1, f2, f3, f4))(MustBePositive).get should equal (Some(30))
  }

  test("find must return None if all the futures are not matching the predicate") {
    val f1 = service3(-10, 100)
    val f2 = service3(-20, 1000)
    val f3 = service3(-30, 3000)
    val f4 = service3(-40, 4000)

    GuavaFutures.find(List(f1, f2, f3, f4))(MustBePositive).get should equal (None)
  }

  test("find must return None if all the futures are failing except one but it still does not matches the predicate") {
    val f1 = failedServiceCall(100L)
    val f2 = failedServiceCall(100L)
    val f3 = service3(-30, 100)
    val f4 = failedServiceCall(100L)

    GuavaFutures.find(List(f1, f2, f3, f4))(MustBePositive).get should equal (None)
  }

  test("find must return the fastest future that comes back and matches the predicate") {
    val now = System.currentTimeMillis
    val f1 = service3(10, 100)
    val f2 = service3(20, 1000)
    val f3 = service3(30, 3000)
    val f4 = service3(40, 4000)

    GuavaFutures.find(List(f1, f2, f3, f4))(MustBePositive).get should equal (Some(10))
    val elapsed = (System.currentTimeMillis - now)
    (elapsed < 900) should be (true)
  }

  test("firstCompletedOf must return None if all the passed futures are failing") {
    val f1 = failedServiceCall(100L)
    val f2 = failedServiceCall(100L)

    GuavaFutures.firstCompletedOf(List(f1, f2)).get should equal (None)
  }

}
