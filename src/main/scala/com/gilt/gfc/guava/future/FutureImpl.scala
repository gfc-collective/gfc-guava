package com.gilt.gfc.guava.future

import java.util.concurrent.{Executor, Future, TimeUnit}

import com.google.common.util.concurrent.{CheckedFuture, ListenableFuture}

/**
 * Stub implementation of the Future interface.
 */
abstract class FutureImpl[T] extends Future[T] {
  override def cancel(mayInterrupt: Boolean): Boolean = false
  override def isCancelled: Boolean = false
  override def isDone: Boolean = false

  override def get: T = get(2, TimeUnit.SECONDS)
  override def get(timeout: Long, unit: TimeUnit): T = get()
}

/**
 * Stub implementation of the ListenableFuture interface.
 */
trait ListenableFutureImpl[T] extends FutureImpl[T] with ListenableFuture[T] {
  override def addListener(listener: Runnable, executor: Executor) { executor.execute(listener) }
}

/**
 * Stub implementation of the CheckedFuture interface.
 */
trait CheckedFutureImpl[T, X <: Exception] extends ListenableFutureImpl[T] with CheckedFuture[T, X] {
  override def checkedGet(timeout: Long, unit: TimeUnit) = checkedGet
}
