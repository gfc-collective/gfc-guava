package com.gilt.gfc.guava.cache

import com.google.common.cache.Cache
import org.scalatest.{Matchers, FunSuite}
import org.mockito.Mockito._

class CacheSetTest extends FunSuite with Matchers {

  test("add") {
    val c0 = mock(classOf[Cache[String, String]])
    val c1 = mock(classOf[Cache[String, String]])

    val cs = new CacheSet

    // add first cache
    cs.add("c0", c0)
    cs.caches shouldEqual Map("c0" -> c0)

    // add another cache with same name
    a [IllegalArgumentException] should be thrownBy {
      cs.add("c0", c1)
      fail("should throw IllegalArgumentException on duplicate add()")
    }

    // add second cache
    cs.add("c1", c1)
    cs.caches shouldEqual Map("c0" -> c0, "c1" -> c1)
  }
}
