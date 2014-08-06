package com.gilt.gfc.guava.cache;

/**
 * Prescribes the initialization strategy for a BulkLoadingCache: should it block until the cache is
 * populated (SYNC), or should it allow cache misses while it bulk-loads in the background (ASYNC)?
 *
 * @author Eric Bowman
 * @since 6/27/12 11:28 AM
 */
public enum CacheInitializationStrategy {
    /**
     * When creating a BulkLoadingCacheFactory, this strategy causes the calling thread to block until
     * the cache has been fully populated.
     */
    SYNC,
    /**
     * When creating a BulkLoadingCacheFactory, this strategy causes the calling thread to return immediately,
     * loading the full cache in the background, and executing the prescribed cache miss behavior until then.
     */
    ASYNC
}
