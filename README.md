# gfc-guava [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gilt/gfc-guava_2.12/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.gilt/gfc-guava_2.12) [![Build Status](https://travis-ci.org/gilt/gfc-guava.svg?branch=master)](https://travis-ci.org/gilt/gfc-guava) [![Coverage Status](https://coveralls.io/repos/gilt/gfc-guava/badge.svg?branch=master&service=github)](https://coveralls.io/github/gilt/gfc-guava?branch=master) [![Join the chat at https://gitter.im/gilt/gfc](https://badges.gitter.im/gilt/gfc.svg)](https://gitter.im/gilt/gfc?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


A library that contains utility classes and scala adapters and adaptations for google guava. Part of the [Gilt Foundation Classes](https://github.com/gilt?q=gfc).

## Getting gfc-guava

The latest version is 0.3.0, which is cross-built against Scala 2.11.x and 2.12.x.

If you're using SBT, add the following line to your build file:

```scala
libraryDependencies += "com.gilt" %% "gfc-guava" % "0.3.0"
```

For Maven and other build tools, you can visit [search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ccom.gilt%20gfc).
(This search will also list other available libraries from the gilt fundation classes.)

## Contents and Example Usage

### com.gilt.gfc.guava.GuavaConverters / GuavaConversions:
These contain implicit and explicit functions to convert between 
* ```com.google.common.base.Optional``` and ```scala.Option``` 
* ```com.google.common.base.Function```, ```com.google.common.base.Supplier```, ```com.google.common.base.Predicate``` and the respective scala functions.

```
    val foo: Option[String] = ???
    
    // Explicit conversion Option -> Optional
    import com.gilt.gfc.guava.GuavaConverters._
    val bar: Optional[String] = foo.asJava
    
    // Implicit conversion Optional -> Option
    import com.gilt.gfc.guava.GuavaConversions._
    val baz: Option[String] = bar
```
```
    // Have an Guava Function
    val guavaFunction: Function[String, Int] = ???
    
    // Explicit conversion Function[String, Int] -> (String => Int)
    import com.gilt.gfc.guava.GuavaConverters._
    val scalaFunction: String => Int = guavaFunction.asScala
    
    // Implicit conversion (String => Int) -> Function[String, Int]
    val guavaFunction2: Function[String, Int] = scalaFunction
```
```
    // Have an Guava Function
    val guavaSupplier: Supplier[String] = ???
    
    // Explicit conversion Supplier[String] -> (() => String)
    import com.gilt.gfc.guava.GuavaConverters._
    val scalaFunction: () => String = guavaSupplier.asScala
    
    // Implicit conversion (() => String) -> Supplier[String]
    val guavaSupplier2: Supplier[String] = scalaFunction _
```
```
    // Have an Guava Predicate
    val guavaPredicate: Predicate[String] = ???
    
    // Explicit conversion Predicate[String] -> (String => Boolean)
    import com.gilt.gfc.guava.GuavaConverters._
    val scalaFunction: String => Boolean = guavaPredicate.asScala
    
    // Implicit conversion (String => Boolean) -> Predicate[String]
    val guavaPredicate2: Predicate[String] = scalaFunction
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
    import com.gilt.gfc.guava.future.GuavaFutures._
    
    // Transform using map
    val f2: ListenableFuture[Int] = future.map(s => s.length)
    
    // Transform using flatMap
    def otherFuture(s: String): Future[Int] = ???
    val f3: ListenableFuture[Int] = future.flatMap(otherFuture)
    
    // Filter
    val f3: ListenableFuture[String] = future.filter(_.length > 0)
    
    // Foreach
    def sideEffect(s:String): Unit = ???
    val f4: ListenableFuture[Int] = future.foreach(sideEffect)
```
* Higher order functions, similar to `scala.Future`
```
    // Have a ListenableFuture
    val future: ListenableFuture[String] = ???
    import com.gilt.gfc.guava.future.GuavaFutures._

    // Handle exceptions that this future might contain
    val safeFuture: ListenableFuture[String] = future.recover {
      case ex: SomeException => "Failed"
    }

    // Asynchronously handle exceptions that this future might contain via a new future
    def fallback(): ListenableFuture[String] = ???
    val safeFuture: ListenableFuture[String] = future.recoverWith {
      case ex: SomeException => fallback()
    }
    
    // Create a new future by applying the 's' function to the successful result of
    // the future, or the 'f' function to the failed result.
    def success(s: String): Int = s.length
    def failure(t: Throwable): Throwable = new SomeException(t)
    val transformedFuture: ListenableFuture[Int] = future.transform(success, failure)
```
* Alternate "fallback" functions to handle failed futures:
```
    // Have a ListenableFuture
    val future: ListenableFuture[String] = ???
    import com.gilt.gfc.guava.future.GuavaFutures._

    // Wrap the future value in a Try and instead of failing the future, fail the Try
    val safeFuture1: ListenableFuture[Try[String]] = future.withTryFallback
    
    // Wrap the future value in an Option and instead of failing the future, return None
    val safeFuture2: ListenableFuture[Option[String]] = future.withOptionFallback()
    
    // Provide a default value, if the future fails:
    val safeFuture3: ListenableFuture[String] = future.withDefault("foo")
```

## License
Copyright 2018 Gilt Groupe, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
