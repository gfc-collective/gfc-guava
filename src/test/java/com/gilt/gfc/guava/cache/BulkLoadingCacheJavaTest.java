package org.gfccollective.guava.cache;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.any;

public abstract class BulkLoadingCacheJavaTest {
    protected abstract <T> void assertEquals(T t1, T t2);
    protected abstract void assertTrue(boolean b);
    protected abstract void assertFalse(boolean b);
    protected abstract void assertNull(Object o);
    protected abstract void fail(String s);

    // in our build, java test code can't see scala test code. :(
    class RunnableCapturingScheduledExecutorMock {
        Runnable runnable = null;
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        public RunnableCapturingScheduledExecutorMock() {
            Mockito.doAnswer(new Answer() {
                public Object answer(InvocationOnMock invocation) throws Exception {
                    Object[] args = invocation.getArguments();
                    runnable = (Runnable)args[0];
                    return null;
                }
            }).when(executor).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        }
    }

    public void testSimpleCreate() throws Exception {
        final AtomicInteger loadCount = new AtomicInteger(0);
        LoadingCache<String, Integer> cache = BulkLoadingCacheFactory.create(
                200,
                new Supplier<Map<String, Integer>>() {
                    public Map<String, Integer> get() {
                        loadCount.incrementAndGet();
                        Map<String, Integer> map = new HashMap<String, Integer>();
                        map.put("USD", "USD".hashCode());
                        map.put("EUR", "EUR".hashCode());
                        return map;
                    }
                }
        );

        assertEquals(loadCount.get(), 1);
        assertNull(cache.getIfPresent("JPY"));
        assertEquals(cache.get("USD"), (java.lang.Integer) "USD".hashCode());
        assertEquals(cache.get("EUR"), (java.lang.Integer) "EUR".hashCode());
        try {
            cache.get("JPY");
            fail("Expected RuntimeException");
        } catch (RuntimeException rte) {
            // test pass
        }
        assertNull(cache.getIfPresent("JPY"));
        Thread.sleep(250);
        assertEquals(loadCount.get(), 2);
        assertNull(cache.getIfPresent("JPY"));
    }

    public void testFullCreate() throws Exception {
        RunnableCapturingScheduledExecutorMock executor = new RunnableCapturingScheduledExecutorMock();
        final AtomicInteger loadCount = new AtomicInteger(0);
        final AtomicInteger missCount = new AtomicInteger(0);
        LoadingCache<String, Integer> cache = BulkLoadingCacheFactory.create(
                1000,
                new Supplier<Map<String, Integer>>() {
                    public Map<String, Integer> get() {
                        loadCount.incrementAndGet();
                        Map<String, Integer> map = new HashMap<String, Integer>();
                        map.put("USD", "USD".hashCode());
                        map.put("EUR", "EUR".hashCode());
                        return map;
                    }
                },
                CacheBuilder.newBuilder(),
                new Function<String, Integer>() {
                    public Integer apply(@Nullable String input) {
                        missCount.incrementAndGet();
                        return input == null ? 0 : input.hashCode();
                    }
                },
                CacheInitializationStrategy.SYNC,
                executor.executor
        );

        assertEquals(loadCount.get(), 1);
        assertNull(cache.getIfPresent("JPY"));
        assertEquals(cache.get("USD"), (java.lang.Integer) "USD".hashCode());
        assertEquals(cache.get("EUR"), (java.lang.Integer) "EUR".hashCode());
        assertEquals(missCount.get(), 0);
        assertEquals(cache.get("JPY"), (java.lang.Integer) "JPY".hashCode());
        assertEquals(cache.getIfPresent("JPY"), (java.lang.Integer) "JPY".hashCode());
        assertEquals(missCount.get(), 1);
        executor.runnable.run();
        assertEquals(loadCount.get(), 2);
        assertNull(cache.getIfPresent("JPY"));
        assertEquals(missCount.get(), 1);
        assertEquals(cache.get("JPY"), (java.lang.Integer) "JPY".hashCode());
        assertEquals(cache.getIfPresent("JPY"), (java.lang.Integer) "JPY".hashCode());
        assertEquals(missCount.get(), 2);
    }

    public void testFullCreateAsync() throws Exception {
        RunnableCapturingScheduledExecutorMock executor = new RunnableCapturingScheduledExecutorMock();
        final AtomicInteger loadCount = new AtomicInteger(0);
        final AtomicInteger missCount = new AtomicInteger(0);
        LoadingCache<String, Integer> cache = BulkLoadingCacheFactory.create(
                1000,
                new Supplier<Map<String, Integer>>() {
                    public Map<String, Integer> get() {
                        loadCount.incrementAndGet();
                        Map<String, Integer> map = new HashMap<String, Integer>();
                        map.put("USD", "USD".hashCode());
                        map.put("EUR", "EUR".hashCode());
                        return map;
                    }
                },
                CacheBuilder.newBuilder(),
                new Function<String, Integer>() {
                    public Integer apply(@Nullable String input) {
                        missCount.incrementAndGet();
                        return input == null ? 0 : input.hashCode();
                    }
                },
                CacheInitializationStrategy.ASYNC,
                executor.executor
        );

        assertEquals(loadCount.get(), 0);
        assertEquals(cache.get("USD"), (java.lang.Integer) "USD".hashCode());
        assertEquals(cache.get("EUR"), (java.lang.Integer) "EUR".hashCode());
        assertEquals(missCount.get(), 2);
        executor.runnable.run();
        assertEquals(loadCount.get(), 1);
        assertEquals(cache.get("USD"), (java.lang.Integer) "USD".hashCode());
        assertEquals(cache.get("EUR"), (java.lang.Integer) "EUR".hashCode());
        assertEquals(missCount.get(), 2);
    }

    public void testCommonUsecaseWith2Indices() throws Exception {
        final AtomicInteger loadCount = new AtomicInteger(0);

        // In "real life" instead of a Supplier<Collection<Foo>> this would be a remote call via a Commons API that returns a Collection<Foo>
        final Supplier<Collection<String>> supplier = new Supplier<Collection<String>>() {
            public Collection<String> get() {
                loadCount.incrementAndGet();
                return Arrays.asList("Alpha", "Beta", "Gamma", "Delta");
            }
        };
        final Function<String, Integer> hashLookup = new Function<String, Integer>() {
            public Integer apply(@Nullable String input) {
                return input.intern().hashCode();
            }
        };
        final Function<String, Character> firstLetterLookup = new Function<String, Character>() {
            public Character apply(@Nullable String input) {
                return input.toLowerCase().charAt(0);
            }
        };

        final LoadingCache<Integer, String> byHashCache = BulkLoadingCacheFactory.create(
                200,
                new Supplier<Map<Integer, String>>() {
                    public Map<Integer, String> get() {
                        return Maps.uniqueIndex(supplier.get(), hashLookup);
                    }
                }
        );

        // This cache doesn't reload from the (remote) supplier but instead uses the values of the 1st cache to build it's index
        final LoadingCache<Character, String> byFirstLetterCache = BulkLoadingCacheFactory.create(
                200,
                new Supplier<Map<Character, String>>() {
                    public Map<Character, String> get() {
                        return Maps.uniqueIndex(byHashCache.asMap().values(), firstLetterLookup);
                    }
                }
        );

        assertEquals(loadCount.get(), 1);
        assertEquals(byHashCache.get(hashLookup.apply("Alpha")), "Alpha");
        assertNull(byHashCache.getIfPresent(123));
        try {
            byHashCache.get(123);
            fail("Expected RuntimeException");
        } catch (RuntimeException rte) {
            // test pass
        }

        assertEquals(byFirstLetterCache.get('a'), "Alpha");
        assertNull(byFirstLetterCache.getIfPresent('c'));
        try {
            byFirstLetterCache.get('c');
            fail("Expected RuntimeException");
        } catch (RuntimeException rte) {
            // test pass
        }
        assertTrue(Optional.fromNullable(byFirstLetterCache.getIfPresent('b')).isPresent());
        assertFalse(Optional.fromNullable(byFirstLetterCache.getIfPresent('e')).isPresent());
        Thread.sleep(250);
        assertEquals(loadCount.get(), 2);
    }
}
