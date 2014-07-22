package com.gilt.gfc.guava.future

import com.google.common.util.concurrent.{AbstractCheckedFuture, CheckedFuture, ListenableFuture}

/**
 * CheckedFuture implementation that wraps a ListenableFuture.
 * Uses an exception mapper function to map exceptions to checked exceptions.
 */
class CheckedFutureWrapper[V, X <: Exception](listenableFuture: ListenableFuture[V])(exceptionMapper: Exception => X) extends AbstractCheckedFuture[V, X](listenableFuture) {
  override def mapException(e: Exception): X = {
    exceptionMapper(e)
  }
}

object CheckedFutureWrapper {
  def apply[V, X <: Exception](listenableFuture: ListenableFuture[V])(exceptionMapper: Exception => X): CheckedFuture[V, X] = {
    new CheckedFutureWrapper[V, X](listenableFuture)(exceptionMapper)
  }
}
