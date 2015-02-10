package com.gilt.gfc.guava.cache

import java.lang.Iterable
import java.util.{Map => JMap}
import java.util.concurrent.{Callable, ScheduledExecutorService, TimeUnit, Executors}
import java.util.concurrent.atomic.AtomicReference
import scala.collection.JavaConverters._
import com.gilt.gfc.guava.GuavaConverters._
import com.gilt.gfc.guava.concurrent.NamedThreadFactory
import com.google.common.base.{Supplier, Function => GFunction}
import com.google.common.cache.{CacheLoader, CacheBuilder, LoadingCache}
import org.slf4j.LoggerFactory

/**
 * Wrapper for Guava's LoadingCache/CacheBuilder API with a bulk cache load and replacement strategy.
 * To use this, at a minimum you pass in a reload period in ms, which dictates how often to reload the cache.
 * Note that this is the amount of time between the end of one reload operation, and the next; this does not try to
 * maintain a constant frequency.  You must also pass in a reloader method, which is called to get an iterator of
 * the key/value pairs to put in the cache.  Finally you may pass in an optional CacheBuilder instance, which is used
 * to actually create the cache, an onMiss function, which is called from the Guava CacheLoader.load method to decide
 * what to do when there is a cache miss (default behavior is a RuntimeException); and an executor which is used to
 * actually schedule cache reloads.
 *
 * Note that caching, and Guava's caching, are hard topics. For example it is rarely a good idea to have a read-back
 * cache in Gilt's business model -- in general we never want to miss cache, which is what this implementation is
 * tuned for. But you may have some luck tweaking specific behavior by passing in your own CacheBuilder. Please do
 * read the Guava documentation carefully before embarking upon advanced use.
 *
 * @author Eric Bowman
 * @since 5/21/12 7:02 AM
 */

trait ShutdownableLoadingCache[K, V] extends LoadingCache[K, V] {
  def shutdown(): Unit
}

trait ManuallyReloadableCache[K, V] extends LoadingCache[K, V] {
  def reload(): Unit
}

object BulkLoadingCache {
  private lazy val defaultExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("BulkLoadingCache", true))

  def apply[K, V](reloadPeriodMs: Long,
                  loadAll: () => Iterator[(K, V)],
                  cacheBuilder: CacheBuilder[_, _] = CacheBuilder.newBuilder,
                  onCacheMiss: K => V = (k: K) => sys.error("Cache miss on %s".format(k)),
                  initStrategy: CacheInitializationStrategy = CacheInitializationStrategy.SYNC,
                  executor: ScheduledExecutorService = defaultExecutor): ShutdownableLoadingCache[K, V] with ManuallyReloadableCache[K, V] = {
    new BulkLoadingCache[K, V](reloadPeriodMs, loadAll, cacheBuilder, onCacheMiss, initStrategy, executor)
  }

  // java api
  def instance[K, V](reloadPeriodMs: Long,
                  loadAll: Supplier[JMap[K, V]],
                  cacheBuilder: CacheBuilder[_, _],
                  onCacheMiss: GFunction[K, V],
                  initStrategy: CacheInitializationStrategy,
                  executor: ScheduledExecutorService): ShutdownableLoadingCache[K, V] with ManuallyReloadableCache[K, V] = {
    apply(reloadPeriodMs, () => loadAll.get().asScala.toIterator, cacheBuilder, onCacheMiss.asScala, initStrategy, executor)
  }

  // java api
  def instance[K, V](reloadPeriodMs: Long,
                  loadAll: Supplier[JMap[K, V]]): ShutdownableLoadingCache[K, V] = {
    apply(reloadPeriodMs, () => loadAll.get().asScala.toIterator)
  }
}


private class BulkLoadingCache[K, V](reloadPeriodMs: Long,
                                     loadAll: () => Iterator[(K, V)],
                                     cacheBuilder: CacheBuilder[_, _],
                                     onCacheMiss: K => V,
                                     initStrategy: CacheInitializationStrategy,
                                     executor: ScheduledExecutorService) extends ShutdownableLoadingCache[K, V] with ManuallyReloadableCache[K, V] {

  private val reference = new AtomicReference[LoadingCache[K, V]]

  private val log = LoggerFactory.getLogger(getClass)


  var initialDelay = 0L
  reference.set {
    initStrategy match {
      case CacheInitializationStrategy.ASYNC =>
        initialDelay = 0
        buildEmptyCache
      case CacheInitializationStrategy.SYNC =>
        initialDelay = reloadPeriodMs
        buildCache
    }
  }

  // then after initialDelay has elapsed, rebuild the cache every reloadPeriodMs.
  // Note we are replace the old cache with a new one, rather than trying to update it.
  // That is somewhat consistent with the idea of by default cache misses are not expected,
  // though certainly not right in the general case.
  val future = executor.scheduleWithFixedDelay(new Runnable {
    def run() {
      try {
        reference.set(buildCache)
      } catch {
        case e: Throwable => log.error("Cache load failed", e)
      }
    }
  }, initialDelay, reloadPeriodMs, TimeUnit.MILLISECONDS)

  // Builds a fresh cache from cacheBuilder and loadAll
  private def buildCache: LoadingCache[K, V] = {
    val newCache = buildEmptyCache

    for ((key, value) <- loadAll()) {
      newCache.put(key, value)
    }
    newCache
  }

  // Builds a fresh cache from cacheBuilder and loadAll
  private def buildEmptyCache: LoadingCache[K, V] = {
    cacheBuilder.asInstanceOf[CacheBuilder[K, V]].build(CacheLoader.from(onCacheMiss.asJava))
  }

  private def wrapped = reference.get()

  override def reload() = reference.set(buildCache)

  override def get(key: K) = wrapped.get(key)

  override def getUnchecked(key: K) = wrapped.getUnchecked(key)

  override def getAll(keys: Iterable[_ <: K]) = wrapped.getAll(keys)

  override def apply(key: K) = wrapped.getUnchecked(key)

  override def refresh(key: K) {
    wrapped.refresh(key)
  }

  override def asMap() = wrapped.asMap()

  override def getIfPresent(key: AnyRef) = wrapped.getIfPresent(key)

  override def get(key: K, valueLoader: Callable[_ <: V]) = wrapped.get(key, valueLoader)

  override def getAllPresent(keys: Iterable[_]) = wrapped.getAllPresent(keys)

  override def put(key: K, value: V) {
    wrapped.put(key, value)
  }

  override def putAll(map: JMap[_ <: K, _ <: V]) {
    wrapped.putAll(map)
  }

  override def invalidate(key: Any) {
    wrapped.invalidate(key)
  }

  override def invalidateAll(keys: Iterable[_]) {
    wrapped.invalidateAll(keys)
  }

  override def invalidateAll() {
    wrapped.invalidateAll()
  }

  override def size() = wrapped.size()

  override def stats() = wrapped.stats()

  override def cleanUp() {
    wrapped.cleanUp()
  }

  override def shutdown() {
    future.cancel(true)
  }
}
