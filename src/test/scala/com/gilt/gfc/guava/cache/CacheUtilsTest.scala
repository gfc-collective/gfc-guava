package com.gilt.gfc.guava.cache

import com.google.common.cache.LoadingCache
import com.google.common.collect.Maps
import org.scalatest.{Matchers, FunSuite}
import org.mockito.Mockito._

class CacheUtilsTest extends FunSuite with Matchers {

  test("Refresh All") {
    val k0 = "k0"
    val k1 = "k1"

    val m = {
      val m0 = Maps.newConcurrentMap[String, String]
      m0.put(k0, k0)
      m0.put(k1, k1)
      m0
    }

    val c = mock(classOf[LoadingCache[String, String]])
    when(c.asMap()).thenReturn(m)

    CacheUtils.refreshAll(c)

    verify(c).refresh(k0)
    verify(c).refresh(k1)
  }
}
