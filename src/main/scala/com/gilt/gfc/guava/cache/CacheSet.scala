package com.gilt.gfc.guava.cache

import com.google.common.cache.{ Cache, CacheStats }
import scala.collection.mutable

/**
 * Keeps named references to a bunch of related Guava Caches.
 *
 * One use case is to declare a singleton and register all permanent caches
 * with it to simplify monitoring/reporting.
 *
 * {{{object AppCacheSet extends CacheSet}}}
 */
class CacheSet {

  private val _caches: mutable.Map[String,  Cache[_, _]] = mutable.Map()

  /**
   * Add a cache to the set with the given name.
   *
   * @throws IllegalArgumentException if name is already used
   */
  def add(name: String, cache: Cache[_, _]) {
    require(!_caches.contains(name), "Cache already registered with key '%s'".format(name))

    _caches.put(name, cache)
  }

  /** Read-only snapshot of caches in the set.
   */
  def caches: Map[String, Cache[_, _]] = _caches.toMap

  /** Flush all caches in the set.
   */
  def invalidateAll() {
    _caches.values.foreach( _.invalidateAll() )
  }

  /** Snapshot of CacheStats for all caches in the set.
   */
  def stats: Map[String,  CacheStats] = caches.map { case (name, cache) =>
    (name -> cache.stats)
  }
}
