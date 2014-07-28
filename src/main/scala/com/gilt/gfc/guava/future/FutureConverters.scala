package com.gilt.gfc.guava.future

import java.util.concurrent.{ExecutionException, Executor, TimeUnit}
import scala.concurrent.{Await, Promise, CanAwait, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.Try
import com.gilt.gfc.util.Throwables
import com.google.common.util.concurrent.{ListenableFuture, CheckedFuture, AbstractCheckedFuture, MoreExecutors}

/**
 * Implicit converters between Scala Future and Guava (Listenable/Checked)Future
 *
 * @author Gregor Heine
 * @since 07/Jul/2014 15:07
 */
object FutureConverters {
  implicit class GuavaFutureConverter[T](val guavaFuture: ListenableFuture[T]) extends AnyVal {
    def asScala: Future[T] = {
      guavaFuture match {
        case MappingCheckedFuture(ScalaFutureAdapter(f), _, _) => f
        case ScalaFutureAdapter(f) => f
        case _ => ListenableFutureAdapter(guavaFuture)
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

    def asCheckedFuture[X <: Exception](implicit exceptionMapper: Exception => X, tag: ClassTag[X]): CheckedFuture[T, X] = {
      new MappingCheckedFuture(asListenableFuture, exceptionMapper, tag)
    }
  }

  object ListenableFutureAdapter {
    def apply[T](guavaFuture: ListenableFuture[T]): ListenableFutureAdapter[T] = {
      val delegate = {
        val promise: Promise[T] = Promise()
        val callbackListener = new Runnable {
          def run {
            try {
              val value = guavaFuture.get
              promise.trySuccess(value)
            } catch {
              case e: ExecutionException if (e.getCause != null) => promise.tryFailure(e.getCause)
              case e: Throwable => promise.tryFailure(e)
            }
          }
        }
        guavaFuture.addListener(callbackListener, MoreExecutors.sameThreadExecutor())
        promise.future
      }
      new ListenableFutureAdapter(delegate, guavaFuture)
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
    override def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext): Future[U] = {
      import com.gilt.gfc.guava.future.GuavaFutures._
      new ListenableFutureAdapter(delegate.recover(pf),
                                  guavaFutureF.recover(pf))
    }
    override def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext): Future[U] = {
      val recoveredScalaFuture = delegate.recoverWith(pf)
      new ListenableFutureAdapter(recoveredScalaFuture,
                                  ScalaFutureAdapter(recoveredScalaFuture))
    }
    override def map[S](f: T => S)(implicit executor: ExecutionContext): ListenableFutureAdapter[S] = {
      import com.gilt.gfc.guava.future.GuavaFutures._
      new ListenableFutureAdapter(delegate.map(f), listenableFuture.map(f))
    }
  }

  case class ScalaFutureAdapter[T](scalaFuture: Future[T]) extends ListenableFuture[T] {
    override def addListener(listener: Runnable, executor: Executor): Unit = {
      scalaFuture.onComplete(_ => listener.run())(ExecutionContext.fromExecutor(executor))
    }
    override def isCancelled: Boolean = false
    override def get(): T = doGet(Await.result(scalaFuture, Duration.Inf))
    override def get(timeout: Long, unit: TimeUnit): T = doGet(Await.result(scalaFuture, Duration.create(timeout, unit)))
    override def cancel(mayInterruptIfRunning: Boolean): Boolean = throw new UnsupportedOperationException("cancel is not supported")
    override def isDone: Boolean = scalaFuture.isCompleted
    def doGet(f: => T) = try(f) catch {
      case ee: ExecutionException => throw(ee)
      case t: Throwable => throw new ExecutionException(t)
    }
  }

  case class MappingCheckedFuture[T, X <: Exception](wrapped: ListenableFuture[T], exceptionMapper: Exception => X, tag: ClassTag[X]) extends AbstractCheckedFuture[T, X](wrapped) {
    override def mapException(exc: Exception): X = {
      val rootExc = Throwables.rootCause(exc)
      if (tag.runtimeClass.isAssignableFrom(rootExc.getClass)) {
        rootExc.asInstanceOf[X]
      } else {
        exceptionMapper(exc)
      }
    }

    override def checkedGet(): T = try {
      delegate.get()
    } catch {
      case ex: Exception => throw mapException(ex)
    }


    override def checkedGet(timeout: Long, unit: TimeUnit): T = try {
      delegate.get(timeout, unit)
    } catch {
      case ex: Exception => throw mapException(ex)
    }
  }
}
