package com.gilt.gfc.guava.cache

import com.google.common.cache.CacheBuilder
import org.scalatest.{Matchers, FunSuite}

class RichCacheBuilderTest extends FunSuite with Matchers {

  test("BuildWithLoader") {
    import CacheConversions._

    def load(key: String): String = {
      "(" + key + ")"
    }

    val b = CacheBuilder.newBuilder
    val c = b.buildWithLoader(load _)

    val i0 = "0"
    val i1 = "1"

    c.get(i0) shouldBe "(0)"
    c.get(i1) shouldBe "(1)"
  }

  test("BuildWithLoader primitive") {
    import CacheConversions._

    def load(key: Long): String = {
      "(" + key + ")"
    }

    val b = CacheBuilder.newBuilder
    val c = b.buildWithLoader(load _)

    val i0 = 0L
    val i1 = 1L

    /* explicitly test that primitive keys work, since there's some
     * ugly casting going on inside buildWithLoader
     */
    c.get(i0) shouldBe "(0)"
    c.get(i1) shouldBe "(1)"
  }
}
