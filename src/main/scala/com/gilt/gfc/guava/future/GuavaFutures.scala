package com.gilt.gfc.guava.future

import java.lang.{Runnable => JRunnable}
import java.util.concurrent.{Callable, ExecutorService}
import java.util.concurrent.atomic.AtomicInteger

import scala.util.control.Exception
import org.slf4j.LoggerFactory
import com.google.common.util.concurrent.{Futures, JdkFutureAdapters, ListenableFuture, MoreExecutors, SettableFuture => GSettableFuture}

/**
 * Rich wrapper providing a monadic interface to Guava ListenableFuture.
 *
 * Checked future is slightly harder but may be possible; we can try to add that
 * at some point.
 *
 * To use, import GuavaFutures._
 *
 * @author Eric Bowman
 * @since 3.0.0
 */
object GuavaFutures {
  import scala.language.implicitConversions
  private val logger = LoggerFactory.getLogger(this.getClass)

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
                logger.error("One of the futures on which I was waiting has just failed!", t)
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
