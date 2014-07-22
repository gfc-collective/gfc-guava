package com.gilt.gfc.guava.future

import java.io.IOException
import java.util.concurrent.{ ExecutionException, Future }
import com.google.common.util.concurrent.{CheckedFuture, ListenableFuture}
import org.scalatest.{Matchers, FunSuite}

class FutureTest extends FunSuite with Matchers {

  test("FutureNow") {
    val f: Future[String] = FutureNow("foo")
    f.get should be ("foo")
    f.isDone should be(true)

    checkExceptionWithMessage[InterruptedException]("blah") {
      FutureNow.fromThrowable(new InterruptedException("blah")).get()
    }

    checkExceptionWithMessage[ExecutionException]("java.io.IOException: blah") {
      FutureNow.fromThrowable(new IOException("blah")).get()
    }
  }

  test("ListenableFutureNow") {
    val f: ListenableFuture[String] = ListenableFutureNow("foo")
    f.get should be ("foo")
    f.isDone should be(true)

    checkExceptionWithMessage[InterruptedException]("blah") {
      ListenableFutureNow.fromThrowable(new InterruptedException("blah")).get()
    }

    checkExceptionWithMessage[ExecutionException]("java.io.IOException: blah") {
      ListenableFutureNow.fromThrowable(new IOException("blah")).get()
    }

    ListenableFutureNow.fromOp { "foo" }.get should be("foo")

    checkExceptionWithMessage[ExecutionException]("java.io.IOException: blah") {
      ListenableFutureNow.fromOp { throw new IOException("blah") }.get()
    }
  }

  test("CheckedFutureNow") {
    val f: CheckedFuture[String, Exception] = CheckedFutureNow("foo")
    f.get should be ("foo")
    f.isDone should be(true)

    checkExceptionWithMessage[InterruptedException]("blah") {
      CheckedFutureNow.fromThrowable(new InterruptedException("blah")).get()
    }
    checkExceptionWithMessage[InterruptedException]("blah") {
      CheckedFutureNow.fromThrowable(new InterruptedException("blah")).checkedGet()
    }

    checkExceptionWithMessage[ExecutionException]("java.io.IOException: blah") {
      CheckedFutureNow.fromThrowable(new IOException("blah")).get()
    }
    checkExceptionWithMessage[IOException]("blah") {
      CheckedFutureNow.fromThrowable(new IOException("blah")).checkedGet()
    }

    CheckedFutureNow.fromOp { "foo" }.get should be("foo")

    checkExceptionWithMessage[IOException]("blah") {
      CheckedFutureNow.fromOp[String, IOException]{ throw new IOException("blah") }.checkedGet()
    }
  }

  private def checkExceptionWithMessage[E <: Throwable](expectedMsg: String)
                                                       (testBody: => Unit)
                                                       (implicit m: Manifest[E]) {
    val exc = the [E] thrownBy { testBody }
    exc.getClass should be (m.runtimeClass)
    exc.getMessage should be(expectedMsg)
  }
}
