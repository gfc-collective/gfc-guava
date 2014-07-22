package com.gilt.gfc.guava.future

import java.util.concurrent.{Executor, TimeUnit}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise, CanAwait, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try
import com.google.common.util.concurrent.{MoreExecutors, CheckedFuture, ListenableFuture}

/**
 * TODO: Document Me.
 *
 * @author Gregor Heine
 * @since 07/Jul/2014 15:07
 */
object FutureConverters {
  implicit class GuavaFutureConverter[T](val guavaFuture: ListenableFuture[T]) extends AnyVal {
    def asScala: Future[T] = {
      guavaFuture match {
        case sfa: ScalaFutureAdapter[T] => sfa.scalaFuture
        case _ =>
          val delegate = {
            val promise: Promise[T] = Promise()
            val callbackListener = new Runnable {
              def run {
                try {
                  val value = guavaFuture.get
                  promise.trySuccess(value)
                } catch {
                  case e: Throwable => {
                    promise.tryFailure(e)
                  }
                }
              }
            }
            guavaFuture.addListener(callbackListener, MoreExecutors.sameThreadExecutor())
            promise.future
          }
          new ListenableFutureAdapter(delegate, guavaFuture)
      }
    }
  }

  implicit class ScalaFutureConverter[T](val scalaFuture: Future[T]) extends AnyVal {
    def asListenableFuture: ListenableFuture[T] = {
      scalaFuture match {
        case fa: ListenableFutureAdapter[T] => fa.listenableFuture
        case _ => new ScalaFutureAdapter(scalaFuture)
      }
    }

    def asCheckedFuture[X <: Exception](implicit unknownExceptionMapper: Throwable => X, tag: ClassTag[X]): CheckedFuture[T, X] = {
      def rootCause(t: Throwable, acc: Set[Throwable] = Set.empty): Throwable = {
        Option(t.getCause).filterNot(acc.contains).fold(t)(t => rootCause(t, acc + t))
      }
      CheckedFutureWrapper(asListenableFuture) { e =>
        val rootExc = rootCause(e)
        if (tag.runtimeClass.isAssignableFrom(rootExc.getClass)) rootExc.asInstanceOf[X] else unknownExceptionMapper(rootExc)
      }
    }
  }
}


class ListenableFutureAdapter[T](delegate: Future[T], guavaFutureF: => ListenableFuture[T]) extends Future[T] {
  def listenableFuture = guavaFutureF
  override def onComplete[U](func: (Try[T]) => U)(implicit executor: ExecutionContext): Unit = delegate.onComplete(func)
  override def isCompleted: Boolean = delegate.isCompleted
  override def value: Option[Try[T]] = delegate.value
  override def result(atMost: Duration)(implicit permit: CanAwait): T = delegate.result(atMost)(permit)
  override def ready(atMost: Duration)(implicit permit: CanAwait): ListenableFutureAdapter.this.type = {
    delegate.ready(atMost)(permit)
    this
  }
  override def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext): Future[U] = {
    def adapt[U] = guavaFutureF.asInstanceOf[ListenableFuture[U]]
    new ListenableFutureAdapter(delegate.recoverWith(pf), adapt).asInstanceOf[Future[U]]
  }
  override def map[S](f: T => S)(implicit executor: ExecutionContext): ListenableFutureAdapter[S] = {
    import com.gilt.gfc.guava.future.GuavaFutures._
    new ListenableFutureAdapter(delegate.map(f), listenableFuture.map(f))
  }
}

class ScalaFutureAdapter[T](val scalaFuture: Future[T]) extends ListenableFuture[T] {
  override def addListener(listener: Runnable, executor: Executor): Unit = {
    scalaFuture.onComplete(_ => listener.run())(ExecutionContext.fromExecutor(executor))
  }
  override def isCancelled: Boolean = false
  override def get(): T = Await.result(scalaFuture, Duration.Inf)
  override def get(timeout: Long, unit: TimeUnit): T = Await.result(scalaFuture, Duration.create(timeout, unit))
  override def cancel(mayInterruptIfRunning: Boolean): Boolean = throw new UnsupportedOperationException("cancel is not supported")
  override def isDone: Boolean = scalaFuture.isCompleted
}
