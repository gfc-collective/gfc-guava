# gfc-guava

A library that contains utility classes and scala adaptations for google guava. Part of the gilt foundation classes.

## Contents and Example Usage

### com.gilt.gfc.guava.GuavaConverters / com.gilt.gfc.guava.GuavaConversions:
These contain implicit and explicit functions to convert between 
* ```com.google.common.base.Optional``` and ```scala.Option``` 
* ```com.google.common.base.Function```, ```com.google.common.base.Supplier```, ```com.google.common.base.Predicate``` and the respective scala functions.

```
    val foo: Option[String] = ???
    
    // explicit conversion Option -> Optional
    import com.gilt.gfc.guava.GuavaConverters._
    val bar: Optional[String] = foo.asJava
    
    // implicit conversion Optional -> Option
    import com.gilt.gfc.guava.GuavaConversions._
    val baz: Option[String] = bar
```

### com.gilt.gfc.guava.RangeHelper:
Helper to convert between ```com.google.common.collect.Range``` and Option tuples [lower-bound, upper-bound):
```
    // Empty Range: None
    val rangeOpt: Option[Range[Int]] = RangeHelper.build(None, None)
    
    // Less-than Range: Some( [Inf...10) )
    val rangeOpt: Option[Range[Int]] = RangeHelper.build(None, Some(10))
    
    // At-least Range: Some( [10...Inf) )
    val rangeOpt: Option[Range[Int]] = RangeHelper.build(Some(10), None)
    
    // Closed-open Range: Some( [10...20) )
    val rangeOpt: Option[Range[Int]] = RangeHelper.build(Some(10), Some(20))
    
    // Singleton Range: Some( [10] )
    val rangeOpt: Option[Range[Int]] = RangeHelper.build(Some(10), Some(10))
```

### com.gilt.gfc.guava.cache.BulkLoadingCache:
  Wrapper for Guava's LoadingCache/CacheBuilder API with a bulk cache load and replacement strategy.
```
    // How often should the cache reload (ms)?
    val reloadPeriodMs: Long = ???
    
    // Have a bulk load function that returns a KV-tuple Iterator
    def loadAll(): Iterator[(Int, String)] = ???
    
    // Build the cache with defaults
    val myCache1 = BulkLoadingCache(reloadPeriodMs, 
                                    loadAll)
```
```
    // Optionally use a customized guava CacheBuilder
    // Default uses an instance with default settings, including strong keys, strong values, 
    // and no automatic eviction of any kind
    val cacheBuilder: CacheBuilder[_, _] = ???
    
    // Optionally have a cache-miss function
    // Default throws a RuntimeException on cache-miss
    def onCacheMiss(key: Int): String = ???
    
    // Optionally specify the cache initialization strategy (sync or async)
    // Default is to initialize the cache synchronously
    val initStrategy = CacheInitializationStrategy.ASYNC
    
    // Optionally have a ScheduledExecutorService
    // Default creates a new scheduled thread pool of size 1
    val executor: ScheduledExecutorService = ???

    // Build the cache fully customized
    val myCache2 = BulkLoadingCache(reloadPeriodMs, 
                                    loadAll, 
                                    cacheBuilder, 
                                    onCacheMiss, 
                                    initStrategy, 
                                    executor)
```
### com.gilt.gfc.guava.future.FutureConverters:
Provides conversions between scala Future and guava ListenableFuture/CheckedFuture instances.
```
    // Have a Scala Future
    val scalaFuture: Future[String] = ???
    
    // Convert to Guava ListenableFuture
    import com.gilt.gfc.guava.future.FutureConverters._
    val listeableFuture: ListenableFuture[String] = scalaFuture.asListenableFuture
    
    // Convert to Guava CheckedFuture: 
    // Needs an implicit Exception mapper to lift Exception to the checked type:
    implicit val em: Exception => SomeException = e: Exception => new SomeException(e)
    val checkedFuture: CheckedFuture[String, SomeException] = scalaFuture.asCheckedFuture
```
```
    // Have a Guava ListenableFuture
    val guavaFuture: ListenableFuture[String] = ???

    // Convert to Guava ListenableFuture
    import com.gilt.gfc.guava.future.FutureConverters._
    val scalaFuture: Future[String] = guavaFuture.asScala
```
__Note__ that Scala Futures that have been converted to Guava/Java Futures like above do _not_ support cancellation and calling .cancel(mayInterruptIfRunning) will throw an UnsupportedOperationException.

### com.gilt.gfc.guava.future.GuavaFutures
A wrapper for Guava ListenableFuture providing monadic operations and higher-order functions similar to the Scala Future object and class.

* Execute a function asynchronously by wrapping it in a ListenableFuture:
```
    // Have an implicit `java.util.concurrent.ExecutorService`
    implicit val es: ExecutorService = ???
    
    // Wrap the asynchronous execution of a function in a ListenableFuture
    val future: ListenableFuture[String] = GuavaFutures.future {
      "Hello"
    }
```
* Find the first succeeded future from the given collection of futures, discarding the others. Returns None if no future completed successfully.
```
    // Have a collection of ListenableFuture:
    val futures: Iterable[ListenableFuture[String]] = ???
    
    // Find the first that succeeds:
    val first: ListenableFuture[Option[String]] = GuavaFutures.firstCompletedOf(futures)
```
* Find the first succeeding future from the given collection of futures, that matches a predicate. Returns None if no future completed successfully.
```
    // Have a collection of ListenableFuture:
    val futures: Iterable[ListenableFuture[String]] = ???
    
    // Find the first that succeeds and matches the predicate ("Hello"):
    val first: ListenableFuture[Option[String]] = GuavaFutures.firstCompletedOf(futures)(_  == "Hello")
```
* Monadic functions, similar to `scala.Future`
```
    // Have a ListenableFuture
    val future: ListenableFuture[String] = ???
    
    // Transform using map
    import com.gilt.gfc.guava.future.GuavaFutures._
    val f2: ListenableFuture[Int] = future.map(s => s.length)
    
    // Transform using flatMap
    def otherFuture(s: String): Future[Int] = ???
    val f3: ListenableFuture[Int] = future.flatMap(otherFuture)
    
    // Filter
    val f3: ListenableFuture[String] = future.filter(_.length > 0)
    
    // Foreach
```
## License
Copyright 2014 Gilt Groupe, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
