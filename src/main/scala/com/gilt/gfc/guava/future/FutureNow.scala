package com.gilt.gfc.guava.future

import java.util.concurrent.ExecutionException
import scala.reflect.ClassTag

/**
 * Adapts simple values to Future interface.
 */
class FutureNow[T](val myVal: T) extends FutureImpl[T] {
  override def isDone: Boolean = true
  override def get(): T = myVal
}

object FutureNow {
  def apply[T](myVal: T) = new FutureNow[T](myVal)

  def fromThrowable[T](throwable: Throwable) = {
    new FutureNow[T](null.asInstanceOf[T]) with ThrowableAdapter {
      override def get() = get(throwable)
    }
  }
}

/**
 * Adapts simple values to ListenableFuture interface.
 */
class ListenableFutureNow[T](myVal: T)
extends FutureNow(myVal)
   with ListenableFutureImpl[T]

object ListenableFutureNow {
  def apply[T](myVal: T) = new ListenableFutureNow[T](myVal)

  def fromOp[T](unsafeOperation: => T): ListenableFutureNow[T] = {
    try {
      apply(unsafeOperation)
    } catch {
      case e: Throwable => fromThrowable(e)
    }
  }

  def fromThrowable[T](throwable: Throwable) = {
    new ListenableFutureNow[T](null.asInstanceOf[T]) with ThrowableAdapter {
      override def get() = get(throwable)
    }
  }
}

/**
 * Adapts simple values to CheckedFuture interface.
 */
class CheckedFutureNow[T, X <: Exception](myVal: T)
extends FutureNow(myVal)
   with CheckedFutureImpl[T, X] {
  override def checkedGet(): T = myVal
}

object CheckedFutureNow {
  def apply[T, X <: Exception](myVal: T) = new CheckedFutureNow[T, X](myVal)

  def fromOp[T, X <: Exception](unsafeOperation: => T)
                               (implicit tag: ClassTag[X]): CheckedFutureNow[T, X] = {
    try {
      apply(unsafeOperation)
    } catch {
      case e: Throwable =>
        if (tag.runtimeClass.isAssignableFrom(e.getClass)) {
          fromThrowable[T, X, X](e.asInstanceOf[X])
        } else {
          throw e // checked future only deals with checked exceptions, Throwable should bubble up
        }
    }
  }

  def fromThrowable[T, X <: Exception, F <: X](throwable: F): CheckedFutureNow[T, X] = {
    new CheckedFutureNow[T, X](null.asInstanceOf[T]) with ThrowableAdapter {
      override def get() = get(throwable)
      override def checkedGet() = throw throwable
    }
  }
}

// common bit of repeated code among fromThrowable implementations
// due to Throwable/Excpeption distinction on the type level it's
// a bit tricky to adapt CheckedFutureNow.fromThrowable(...) call
// to be used by FutureNow.fromThrowable(...)
private[future] trait ThrowableAdapter {
  final def get(throwable: Throwable) = throwable match {
    case ie: InterruptedException => throw ie
    case e => throw new ExecutionException(throwable)
  }
}
