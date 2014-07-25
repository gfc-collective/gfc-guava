package com.gilt.gfc.guava.future

import java.lang.{ Runnable => JRunnable }
import java.util.{ Collection => JCollection }
import java.util.concurrent.{Callable, ExecutorService}
import java.util.concurrent.atomic.AtomicInteger
import scala.util.control.Exception
import com.gilt.gfc.logging.OpenLoggable
import com.google.common.base.{ Function => GFunction, Predicate => GPredicate, Optional }
import com.google.common.util.concurrent.{ JdkFutureAdapters, Futures, MoreExecutors, ListenableFuture, FutureFallback, SettableFuture => GSettableFuture }

/**
 * Rich wrapper providing a monadic interface to Guava ListenableFuture.
 *
 * Checked future is slightly harder but may be possible; we can try to add that
 * at some point.
 *
 * To use, import GuavaFutures._. This will bring an implicit into scope which will wrap a ListenableFuture with
 * a RichListenableFuture, which provides the standard monadic operations (map, flatMap, foreach & withFilter)
 * for ListenableFuture. See the tests for some examples how to use.  This is basically to bring some of the
 * benefits of Akka futures to Guava.
 *
 * @author Eric Bowman
 * @since 3.0.0
 */
object GuavaFutures {
  import scala.language.implicitConversions

  implicit def lf2rlf[T](listenableFuture: ListenableFuture[T]): RichListenableFuture[T] = {
    new RichListenableFuture[T](listenableFuture)
  }

  /**
   * Sugar for getting hold of a ListenableFuture backing a lifted execution against
   * an implicit executor. Example:
   *
   * implicit val executor = Executors.newCachedThreadPool()
   *
   * val future: ListenableFuture[Int] = future { Thread.sleep(5000); 5 }
   *
   * @author Eric Bowman
   * @since 11/24/12 7:20 PM
   */
  def future[T](f: => T)(implicit executor: ExecutorService): ListenableFuture[T] = {
    JdkFutureAdapters.listenInPoolThread(
      executor.submit[T](new Callable[T] {
        def call() = f
      }), executor)
  }

  /**
   * Returns the first succeeded future from the given collection discarding the others.
   *
   * @param futures a collection of futures housing a result of type T
   * @return a future holding an optional T that is the first succeded future from the passed iterable or None if
   * no future completed succesfully.
   */
  def firstCompletedOf[T](futures: JCollection[ListenableFuture[T]]): ListenableFuture[Optional[T]] = firstCompletedAndMatchingOf(futures) { t: T => true }

  /**
   * Returns the first succedding future that matches the predicate.
   *
   * @param futures a collection of futures.
   * @param predicate the predicate that has to be matched from the result of the futures.
   * @return a future of an optional T that is the result of the first succeding future that also matches the predicate
   * or None otherwise.
   */
  def find[T](futures: JCollection[ListenableFuture[T]], predicate: GPredicate[T]): ListenableFuture[Optional[T]] =
    firstCompletedAndMatchingOf(futures) { t: T => predicate.apply(t) }

  private def firstCompletedAndMatchingOf[T](futures: JCollection[ListenableFuture[T]])(predicate: T => Boolean): ListenableFuture[Optional[T]] = {
    import scala.collection.JavaConverters._

    val futuresToWaitFor = futures.asScala.toList

    if (futuresToWaitFor.isEmpty) {
      Futures.immediateFuture(Optional.absent[T])
    } else {
      val promise: GSettableFuture[Optional[T]] = GSettableFuture.create[Optional[T]]
      val failedFuturesSoFar = new AtomicInteger(0)

      futuresToWaitFor.foreach { f =>
        f.addListener(new JRunnable() {
          override def run() {
            val result: Option[T] = Exception.catching(classOf[Exception]).withApply { t: Throwable =>
              if (!f.isCancelled()) {
                RichListenableFuture.error("One of the futures on which I was waiting has just failed!", t)
              }
              None
            } {
              val t: T = f.get // this is not blocking: f is aready resolved because we are executing its listener
              if (predicate(t)) Some(t) else None
            }

            // All the following code assumes that only the first set on
            // the SettableFuture sets it (as per Guava's javadoc).
            if (result.isDefined && promise.set(Optional.of(result.get))) { // only the first set will succeed
              futuresToWaitFor.foreach { f: java.util.concurrent.Future[T] =>
                f.cancel(false)
              }
            } else {
              if (failedFuturesSoFar.incrementAndGet() == futuresToWaitFor.size) {
                promise.set(Optional.absent[T]) // all the futures failed (errors or not matching the predicate): returning None
              }
            }
          }
        }, MoreExecutors.sameThreadExecutor())
      }

      promise
    }
  }
}

private object RichListenableFuture extends OpenLoggable

case class RichListenableFuture[T](future: ListenableFuture[T]) {

  import RichListenableFuture._

  def map[A](f: T => A): ListenableFuture[A] = {
    Futures.transform[T, A](future, new GFunction[T, A] {
      def apply(v1: T) = f(v1)
    })
  }

  def flatMap[A](f: T => ListenableFuture[A]): ListenableFuture[A] = {
    import com.gilt.gfc.guava.future.GuavaFutures._
    Futures.dereference[A](map(f).map(_.future).future)
  }

  def foreach[U](f: T => Unit) {
    map(f)
  }

  def filter(q: T => Boolean): ListenableFuture[T] = {
    map {
      case t if q(t) => t
    }
  }

  def withFilter(q: T => Boolean): ListenableFuture[T] = {
    map {
      case t if q(t) => t
    }
  }

  /**
   * Helper method to guard against RPC failures and the like,
   * converts a 'regular' Future to a Future of Either
   * with Left[Throwable] representing cases where an exception was
   * caught and Right[T] representing desired results.
   */
  def withEitherFallback: ListenableFuture[Either[Throwable, T]] = {
    val fallBackToLeft = new FutureFallback[Either[Throwable, T]] {
      override def create(t: Throwable): ListenableFuture[Either[Throwable, T]] = {
        Futures.immediateFuture(Left(t))
      }
    }
    Futures.withFallback(map(Right(_)), fallBackToLeft)
  }

  /**
   * Helper method to guard against RPC failures and the like,
   * converts a 'regular' Future to a Future of Option.
   * Variation of withEitherFallback method where None represents
   * an error (exception was thrown) case and Some[T] represents
   * an Ok case.
   *
   * Optional errorCallback parameter allows caller to inject
   * e.g. custom error logging, like
   * {{{
   *  class Foo extends Loggable {
   *    f.withOptionFallback(error(_))
   *  }
   * }}}
   *
   * If no errorCallback is specified the exception is logged as an error.
   */
  def withOptionFallback(errorCallback: (Throwable) => Unit = { ex: Throwable => error(ex)}): ListenableFuture[Option[T]] = {
    val fallBackToNone = new FutureFallback[Option[T]] {
      override def create(t: Throwable): ListenableFuture[Option[T]] = {
        errorCallback(t) // to avoid embedding error logging here
        Futures.immediateFuture(None)
      }
    }
    Futures.withFallback(map(Option(_)), fallBackToNone)
  }

  /**
   * Helper method to guard against RPC failures and the like.
   * Variation of withEitherFallback method where a default value is returned
   * in cases where there an exception is thrown.
   *
   * If no errorCallback is specified the exception is logged as an error.
   */
  def withDefault(fallbackValue: T, errorCallback: (Throwable) => Unit = { ex: Throwable => error(ex)}): ListenableFuture[T] = {
    val fallBack = new FutureFallback[T] {
      override def create(t: Throwable): ListenableFuture[T] = {
        errorCallback(t) // to avoid embedding error logging here
        Futures.immediateFuture(fallbackValue)
      }
    }
    Futures.withFallback(future, fallBack)
  }

  /** Creates a new future that will handle any matching throwable that this
    *  future might contain. If there is no match, or if this future contains
    *  a valid result then the new future will contain the same.
    */
  def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutorService = MoreExecutors.sameThreadExecutor()): ListenableFuture[U] = {
    val fallBack = new FutureFallback[U] {
      override def create(t: Throwable): ListenableFuture[U] = {
        GuavaFutures.future(pf.applyOrElse(t, { t: Throwable => throw t }))
      }
    }
    Futures.withFallback(future, fallBack, executor)
  }

  /** Creates a new future that will handle any matching throwable that this
    *  future might contain by assigning it a value of another future.
    *
    *  If there is no match, or if this future contains
    *  a valid result then the new future will contain the same result.
    */
  def recoverWith[U >: T](pf: PartialFunction[Throwable, ListenableFuture[U]])(implicit executor: ExecutorService = MoreExecutors.sameThreadExecutor()): ListenableFuture[U] = {
    val fallBack = new FutureFallback[U] {
      override def create(t: Throwable): ListenableFuture[U] = {
        pf.applyOrElse(t, { t: Throwable => Futures.immediateFailedFuture(t) })
      }
    }
    Futures.withFallback(future, fallBack, executor)
  }
}