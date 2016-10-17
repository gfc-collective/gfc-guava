package com.gilt.gfc.guava.cache

import java.lang.{Integer => JInt}
import java.util.concurrent.{TimeUnit, ScheduledExecutorService}
import com.google.common.cache.CacheBuilder
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, FunSuite}
import org.scalatest.mock.MockitoSugar

/**
 * Basic sanity tests of BulkLoadingCache.
 *
 * @author Eric Bowman
 * @since 5/21/12 7:41 AM
 */

// Lets us mock the scheduled executor. This captures the runnable
// to execute periodically, and exposes it. This way we can call it at
// specific times in a deterministic way.
class RunnableCapturingScheduledExecutorMock extends MockitoSugar {
  val executor = mock[ScheduledExecutorService]
  var runnable: Runnable = _
  doAnswer(new Answer[AnyRef] {
    def answer(invocation: InvocationOnMock): AnyRef = {
      val args = invocation.getArguments
      runnable = args(0).asInstanceOf[Runnable]
      null
    }
  }).when(executor).scheduleWithFixedDelay(any[Runnable], anyLong, anyLong, any[TimeUnit])
}

class BulkLoadingCacheTest extends FunSuite with Matchers with MockitoSugar {

  val immediateExecutor = new RunnableCapturingScheduledExecutorMock
  test("Basics") {
    var count = 0

    val cache = BulkLoadingCache(
      reloadPeriodMs = 2000,
      loadAll = () => {
        count += 1
        (for (i <- 1 to 10) yield {
          i.toString -> count.toString
        }).toIterator
      },
      executor = immediateExecutor.executor)

    for (i <- 1 to 10) {
      cache.get(i.toString) should equal(1.toString)
    }

    immediateExecutor.runnable.run()

    for (i <- 1 to 10) {
      cache.get(i.toString) should equal(2.toString)
    }

    a [RuntimeException] should be thrownBy {
      cache.get("not ")
    }
  }

  test("Null Miss Is Miss") {
    // note that you can't actually store null, so if you want that behavior,
    // you need to have a NULL reference of your own. Not clear how to avoid storing
    // that though.
    val NULL = new AnyRef
    val cache = BulkLoadingCache(
      1000,
      () => {
        (for (i <- 1 to 10) yield {
          i.toString -> i.toString
        }).toIterator
      },
      CacheBuilder.newBuilder,
      (k: AnyRef) => NULL
    )

    cache.get("7") should equal("7")
    cache.get("11") should equal(NULL)
  }

  test("Reload") {
    var count = 0

    val cache = BulkLoadingCache(
      reloadPeriodMs = 200000,
      loadAll = () => {
        count += 1
        (for (i <- 1 to 10) yield {
          i.toString -> count.toString
        }).toIterator
      },
      executor = immediateExecutor.executor)

    for (i <- 1 to 10) {
      cache.get(i.toString) should equal(1.toString)
    }

    cache.reload()

    for (i <- 1 to 10) {
      cache.get(i.toString) should equal(2.toString)
    }
  }

  test("Custom Caching") {

    // prebuild a cache of string -> hash code, and confirm that hits and misses work as expected
    val immediateExecutor = new RunnableCapturingScheduledExecutorMock
    val builder = CacheBuilder.newBuilder
    var missCount = 0   // counts how many times onCacheMiss has been called (all on the main thread)
    def onCacheMiss(str: String): JInt = {
      missCount += 1
      str.##
    }

    // hash codes for USD and GBP are precomputed; the rest read-back to the onCacheMiss function
    val loadAll: () => Iterator[(String, JInt)] =
      () => List("USD", "GBP").map(k => (k, onCacheMiss(k))).toIterator

    val cache = BulkLoadingCache(1000, loadAll, builder, onCacheMiss _, CacheInitializationStrategy.SYNC, immediateExecutor.executor)

    // to start out, we precomputed hashes for 2 strings, USD and GBP
    assert(missCount === 2)

    // confirm that we can ask does the cache hold something without fetching it
    cache.getIfPresent("JPY") should equal(null)

    // asking for the hash for JPY should compute it exactly once
    assert(cache.get("JPY") === "JPY".##)
    assert(missCount === 3)
    assert(cache.get("JPY") === "JPY".##)
    assert(missCount === 3)

    // USD and GBP should not read-back
    assert(cache.get("USD") === "USD".##)
    assert(missCount === 3)
    assert(cache.get("GBP") === "GBP".##)
    assert(missCount === 3)

    // build a new cache by simulating the scheduled runnable having fired
    immediateExecutor.runnable.run()

    // we should have precomputed hashes for USD and GBP again
    assert(missCount === 5)

    // JPY should miss the cache exactly once
    assert(cache.get("JPY") === "JPY".##)
    assert(missCount === 6)
    assert(cache.get("JPY") === "JPY".##)
    assert(missCount === 6)

    // USD and GBP should not miss the cache
    assert(cache.get("USD") === "USD".##)
    assert(missCount === 6)
    assert(cache.get("GBP") === "GBP".##)
    assert(missCount === 6)


    // Build a cache that loads asynchronously the first time
    val asyncCache = BulkLoadingCache(1000, loadAll, builder, onCacheMiss _, CacheInitializationStrategy.ASYNC, immediateExecutor.executor)

    // there should have been no load yet
    assert(missCount === 6)
    assert(asyncCache.get("USD") === "USD".##)
    assert(missCount === 7)

    // Now trigger the load
    immediateExecutor.runnable.run()

    // we should have precomputed hashes for USD and GBP again
    assert(missCount === 9)
    assert(asyncCache.get("USD") === "USD".##)
    assert(missCount === 9)
  }

  test("Scala Native Types") {
    val cache = BulkLoadingCache[String, Int](1000, () => List("a", "b", "c").map(i => i -> i.##).toIterator)
    cache.get("a") should equal("a".##)
  }

  val javaTest = new BulkLoadingCacheJavaTest {
    override protected def assertEquals[T](t1: T, t2: T): Unit = t1 shouldBe t2
    override protected def assertFalse(b: Boolean): Unit = b shouldBe false
    override protected def assertTrue(b: Boolean): Unit = b shouldBe true
    override protected def assertNull(o: AnyRef): Unit = o shouldEqual null
    override protected def fail(s: String): Unit = BulkLoadingCacheTest.this.fail(s)
  }

  test("Java testSimpleCreate") {
    javaTest.testSimpleCreate()
  }

  test("Java testFullCreate") {
    javaTest.testFullCreate()
  }

  test("Java testFullCreateAsync") {
    javaTest.testFullCreateAsync()
  }

  test("Java testCommonUsecaseWith2Indices") {
    javaTest.testCommonUsecaseWith2Indices()
  }
}
