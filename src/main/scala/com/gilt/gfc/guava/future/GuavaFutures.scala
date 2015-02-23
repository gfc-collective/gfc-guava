package com.gilt.gfc.guava.future

import java.lang.{ Runnable => JRunnable }
import java.util.concurrent.{Callable, ExecutorService}
import java.util.concurrent.atomic.AtomicInteger
import scala.util.{Success, Failure, Try}
import scala.util.control.Exception
import com.gilt.gfc.logging.OpenLoggable
import com.google.common.base.{ Function => GFunction}
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
   * @return a future holding an optional T that is the first succeeded future from the passed iterable or None if
   * no future completed successfully.
   */
  def firstCompletedOf[T](futures: Iterable[ListenableFuture[T]]): ListenableFuture[Option[T]] = find(futures)(_  => true)

  /**
   * Returns the first succeeding future that matches the predicate.
   *
   * @param futures a collection of futures.
   * @param predicate the predicate that has to be matched from the result of the futures.
   * @return a future of an optional T that is the result of the first succeding future that also matches the predicate
   * or None otherwise.
   */
  def find[T](futures: Iterable[ListenableFuture[T]])(predicate: T => Boolean): ListenableFuture[Option[T]] = {
    if (futures.isEmpty) {
      Futures.immediateFuture(None)
    } else {
      val promise = GSettableFuture.create[Option[T]]
      val failedFuturesSoFar = new AtomicInteger(0)

      futures.foreach { f =>
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
            if (result.isDefined && promise.set(Some(result.get))) { // only the first set will succeed
              futures.foreach { f: java.util.concurrent.Future[T] =>
                f.cancel(false)
              }
            } else {
              if (failedFuturesSoFar.incrementAndGet() == futures.size) {
                promise.set(None) // all the futures failed (errors or not matching the predicate): returning None
              }
            }
          }
        }, MoreExecutors.directExecutor())
      }

      promise
    }
  }
}

private object RichListenableFuture extends OpenLoggable

@deprecated("Convert a ListenableFuture to a Scala Future instead", "0.1.2")
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
  @deprecated("Use withTryFallback", "0.1.0")
  def withEitherFallback: ListenableFuture[Either[Throwable, T]] = {
    val fallBackToLeft = newFallBack(t => Futures.immediateFuture(Left(t)))
    Futures.withFallback(map(Right(_)), fallBackToLeft)
  }

  /**
   * Helper method to guard against RPC failures and the like,
   * converts a 'regular' Future to a Future of Try
   * with Failure representing cases where an exception was
   * caught and Success[T] representing desired results.
   */
  def withTryFallback: ListenableFuture[Try[T]] = {
    val fallBackToLeft = newFallBack(t => Futures.immediateFuture(Failure(t)))
    Futures.withFallback(map(Success(_)), fallBackToLeft)
  }

  /**
   * Helper method to guard against RPC failures and the like,
   * converts a 'regular' Future to a Future of Option.
   * Variation of withTryFallback method where None represents
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
    val fallBackToNone = newFallBack { t =>
      errorCallback(t) // to avoid embedding error logging here
      Futures.immediateFuture(None)
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
    val fallBack = newFallBack { t =>
      errorCallback(t) // to avoid embedding error logging here
      Futures.immediateFuture(fallbackValue)
    }
    Futures.withFallback(future, fallBack)
  }

  /** Creates a new future that will handle any matching throwable that this
    *  future might contain. If there is no match, or if this future contains
    *  a valid result then the new future will contain the same.
    */
  def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutorService = MoreExecutors.sameThreadExecutor()): ListenableFuture[U] = {
    val fallBack = newFallBack { t =>
      GuavaFutures.future(pf.applyOrElse(t, (t: Throwable) => throw t))
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
    val fallBack: FutureFallback[U] = newFallBack { t =>
      pf.applyOrElse(t, (t: Throwable) => Futures.immediateFailedFuture(t))
    }
    Futures.withFallback(future, fallBack, executor)
  }

  /** Creates a new future by applying the 's' function to the successful result of
    *  this future, or the 'f' function to the failed result. If there is any non-fatal
    *  exception thrown when 's' or 'f' is applied, that exception will be propagated
    *  to the resulting future.
    *
    *  @param  s  function that transforms a successful result of the receiver into a
    *             successful result of the returned future
    *  @param  f  function that transforms a failure of the receiver into a failure of
    *             the returned future
    *  @return    a future that will be completed with the transformed value
    */
  def transform[U](s: T => U, f: Throwable => Throwable)(implicit executor: ExecutorService = MoreExecutors.sameThreadExecutor()): ListenableFuture[U] = {
    val fallBack: FutureFallback[T] = newFallBack(t => Futures.immediateFailedFuture(f(t)))
    import GuavaFutures.lf2rlf
    Futures.withFallback(future, fallBack, executor).map(s)
  }

  private def newFallBack[T](f: Throwable => ListenableFuture[T]): FutureFallback[T] = new FutureFallback[T] {
    override def create(t: Throwable): ListenableFuture[T] = f(t)
  }
}
