package com.gilt.gfc.guava.cache

import com.google.common.cache.{ Cache, CacheBuilder, LoadingCache }

/**
 * Implicit conversions to/from Guava Cache & CacheBuilder
 */
object CacheConversions {
  import scala.language.implicitConversions

  implicit def cacheToRichCache[K, V](c: Cache[K, V]) = new RichCache[K, V, Cache[K, V]](c)
  implicit def richCacheToCache[K, V](rc: RichCache[K, V, Cache[K, V]]): Cache[K, V] = rc.self

  implicit def loadingCacheToRichLoadingCache[K, V](c: LoadingCache[K, V]) = new RichLoadingCache[K, V](c)
  implicit def richLoadingCacheToLoadingCache[K, V](rc: RichLoadingCache[K, V]): LoadingCache[K, V] = rc.self

  implicit def cacheBuilderToRichCacheBuilder[K, V](cb: CacheBuilder[K, V]) = new RichCacheBuilder[K, V](cb)
  implicit def richCacheBuilderToCacheBuilder[K, V](rcb: RichCacheBuilder[K, V]): CacheBuilder[K, V] = rcb.self
}
