package com.gilt.gfc.guava.cache

import java.util.concurrent.Callable

import com.google.common.cache.{ Cache, LoadingCache }

/**
 * Scala-friendly methods for Guava Cache
 */
@deprecated("Use BulkLoadingCache", "0.0.9")
class RichCache[K, V, C <: Cache[K, V]](val self: C) extends Proxy {

  /**
   * Like Cache.get, but specifies the loader as a scala by-name param
   */
  def getOrLoad(key: K)(valueLoader: => V): V = {
    self.get(key, new Callable[V] {
      def call() = valueLoader
    })
  }
}

@deprecated("Use BulkLoadingCache", "0.0.9")
class RichLoadingCache[K, V](_self: LoadingCache[K, V])
  extends RichCache[K, V, LoadingCache[K, V]](_self)
{

  def refreshAll() {
    CacheUtils.refreshAll(self)
  }
}
