package com.gilt.gfc.guava.cache

import com.google.common.cache.{ CacheBuilder, CacheLoader, LoadingCache }

/**
 * Scala-friendly methods for Guava CacheBuilder
 */
@deprecated("Use BulkLoadingCache", "0.0.9")
class RichCacheBuilder[K0, V0](val self: CacheBuilder[K0, V0]) extends Proxy {

  /**
   * Like CacheBuilder.build(CacheLoader) with a scala function to use as the
   * CacheLoader.  Example:
   *
   * {{{ builder.buildWithLoader { id: Long => db.find(id) } }}}
   *
   * NOTE: In order to allow creation of Caches with primitive keys and
   * values from scala code, this method skirts CacheBuilder's typesafety
   * precautions.  Be careful when using `CacheBuilder.removalListener()`
   * or `CacheBuilder.weigher()` that your arguments to those methods are
   * compatible with your loader (the compiler will no longer check that
   * for you).
   */
  def buildWithLoader[K, V](loader: K => V): LoadingCache[K, V] = {
    import com.gilt.gfc.guava.GuavaConverters._
    self.asInstanceOf[CacheBuilder[K, V]].build(CacheLoader.from(loader.asJava))
  }
}
