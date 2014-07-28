package com.gilt.gfc.guava.cache

import com.google.common.base.{Function => GFunction}
import com.google.common.cache.{Cache, CacheBuilder, CacheLoader, LoadingCache}
import org.scalatest.{Matchers, FunSuite}

class RichCacheTest extends FunSuite with Matchers {

  test("get or load") {
    import CacheConversions._

    def load(in: String): String = "(" + in + ")"

    // test RichCache.getOrLoad
    val c = newCache()
    val v = c.getOrLoad("0") { load("0") }

    // test RichLoadingCache.getOrLoad
    val lf = new GFunction[String, String] {
      def apply(in: String): String = load(in)
    }
    val lc = newLoadingCache(lf)
    val lv = lc.getOrLoad("1") { load("1") }
  }

  private def newCache(): Cache[String, String] = {
    CacheBuilder.newBuilder.build[String, String]()
  }

  private def newLoadingCache(f: GFunction[String, String]): LoadingCache[String, String] = {
    val cl = CacheLoader.from(f)
    CacheBuilder.newBuilder.build(cl)
  }
}
