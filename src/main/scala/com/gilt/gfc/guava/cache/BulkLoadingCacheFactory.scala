package com.gilt.gfc.guava.cache

import java.util.{Map => JMap}
import java.util.concurrent.ScheduledExecutorService
import com.google.common.base.{Function, Supplier}
import com.google.common.cache.{CacheBuilder, LoadingCache}


/**
 * Java factory API for creating a BulkLoadingCache, which is a LoadingCache that periodically bulk-loads and
 * atomically updates itself.
 *
 * @author Eric Bowman
 * @since 5/28/12 4:07 PM
 */
object BulkLoadingCacheFactory {

    /**
     * Factory method for creating a new BulkLoadingCache from Java.
     *
     * @return new LoadingCache that bulk loads.
     */
    def create[K, V](reloadPeriodMs: Long, loadAll: Supplier[JMap[K, V]]): LoadingCache[K, V]  = {
      BulkLoadingCache.instance(reloadPeriodMs, loadAll)
    }

    /**
     * Factory method for creating a new BulkLoadingCache from Java.
     *
     * @return new LoadingCache that bulk loads.
     */
    def create[K, V](reloadPeriodMs: Long,
                     loadAll: Supplier[JMap[K, V]],
                     cacheBuilder: CacheBuilder[_, _],
                     onCacheMiss: Function[K, V],
                     initStrategy: CacheInitializationStrategy,
                     executor: ScheduledExecutorService): LoadingCache[K, V] = {
      BulkLoadingCache.instance(reloadPeriodMs, loadAll, cacheBuilder, onCacheMiss, initStrategy, executor)
    }
}
