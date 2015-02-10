package com.gilt.gfc.guava.cache

import com.google.common.cache.LoadingCache

import scala.collection.JavaConversions._

/**
 * Utils for Guava Cache
 */
@deprecated("Use BulkLoadingCache", "0.0.9")
object CacheUtils {

  /** Refresh all keys in cache (the refresh happens asynchronously)
   */
  def refreshAll[K](cache: LoadingCache[K, _]) {
    cache.asMap.foreach { case (key, _) => cache.refresh(key) }
  }
}
