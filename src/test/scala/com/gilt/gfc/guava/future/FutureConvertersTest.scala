package com.gilt.gfc.guava.future

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutionException, TimeUnit, Executor}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import com.google.common.util.concurrent.{ListenableFuture, Futures}
import org.mockito.ArgumentMatchers.{any, eq => matchEq}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

object FutureConvertersTest {
  implicit class AwaitableFuture[A](val f: Future[A]) extends AnyVal {
    @inline def await: A = Await.result(f, Duration.Inf)
  }
}

class FutureConvertersTest extends AnyFunSuite with Matchers with MockitoSugar {
  import FutureConverters._
  import FutureConvertersTest._
  import scala.concurrent.ExecutionContext.Implicits.global

  class SomeException(msg: String = null, t: Throwable = null) extends Exception(msg, t)
  class SomeOtherException extends Exception

  test("ScalaFuture to ListenableFuture returns the original value") {
    val future = Future.successful(1)
    future.asListenableFuture.get() shouldBe 1
  }

  test("ScalaFuture to ListenableFuture throws the original exception") {
    val exc = new SomeException()
    val future = Future.failed(exc)

    val thrown = the[ExecutionException] thrownBy {
      future.asListenableFuture.get()
    }
    thrown.getCause shouldBe exc
  }

  test("ListenableFuture to ScalaFuture returns the original value") {
    val future = Futures.immediateFuture(1)
    future.asScala.await shouldBe 1
  }

  test("ListenableFuture to ScalaFuture throws the original exception") {
    val exc = new SomeException()
    val future: ListenableFuture[Int] = Futures.immediateFailedFuture(exc)

    val thrown = the[SomeException] thrownBy {
      future.asScala.await
    }
    thrown shouldBe exc
  }

  test("ScalaFuture to ListenableFuture to ScalaFuture returns original ScalaFuture") {
    val future = Future.successful(1)
    future.asListenableFuture.asScala should be theSameInstanceAs future
  }

  test("ListenableFuture to ScalaFuture to ListenableFuture returns original ListenableFuture") {
    val future = Futures.immediateFuture(1)
    future.asScala.asListenableFuture should be theSameInstanceAs future
  }

  test("ScalaFutureAdapter functions") {
    val future = mock[Future[Int]]
    when (future.isCompleted).thenReturn(true)

    val wrapped = future.asListenableFuture
    wrapped.addListener(mock[Runnable], mock[Executor])

    verify(future).onComplete(any())(any())

    wrapped.get()
    verify(future).result(matchEq(Duration.Inf))(any())

    wrapped.get(100L, TimeUnit.MILLISECONDS)
    verify(future).result(matchEq(Duration.create(100L, TimeUnit.MILLISECONDS)))(any())

    a [UnsupportedOperationException] shouldBe thrownBy {
      wrapped.cancel(true)
    }

    wrapped.isDone shouldBe true
    verify(future).isCompleted

    wrapped.isCancelled shouldBe false

    verifyNoMoreInteractions(future)
  }

  test("ScalaFuture.map should map ListenableFuture") {
    val gFuture = Futures.immediateFuture(1)
    val sFuture = gFuture.asScala

    val mappedFuture = sFuture.map(_.toString)
    mappedFuture.await shouldBe "1"
    mappedFuture should not be a [ListenableFutureAdapter[_]]
    val mappedListenableFuture = mappedFuture.asListenableFuture
    mappedListenableFuture.get shouldBe "1"
  }

  test("ScalaFuture.map should call map function only once") {
    val gFuture = Futures.immediateFuture(1)
    val sFuture = gFuture.asScala

    val count = new AtomicInteger(0)
    val mappedFuture = sFuture.map{ i =>
      count.incrementAndGet()
      i.toString
    }
    mappedFuture.await shouldBe "1"
    mappedFuture.asListenableFuture.get shouldBe "1"
    count.get shouldBe 1
  }

  test("ScalaFuture.recover should recover ListenableFuture") {
    val gFuture: ListenableFuture[Int] = Futures.immediateFailedFuture(new Exception)
    val sFuture = gFuture.asScala

    val recoveredFuture = sFuture.recover { case ex: Exception => 2 }
    recoveredFuture.await shouldBe 2
    recoveredFuture should not be a [ListenableFutureAdapter[_]]
    val recoveredListenableFuture = recoveredFuture.asListenableFuture
    recoveredListenableFuture.get shouldBe 2
  }

  test("ScalaFuture.recover should throw same Exception in ListenableFuture") {
    val gFuture: ListenableFuture[Int] = Futures.immediateFailedFuture(new SomeException)
    val sFuture = gFuture.asScala

    val recoverFail = new SomeOtherException
    val recoveredFuture = sFuture.recover { case ex: SomeException => throw recoverFail }
    a [SomeOtherException] should be thrownBy recoveredFuture.await
    recoveredFuture should not be a [ListenableFutureAdapter[_]]
    val recoveredListenableFuture = recoveredFuture.asListenableFuture
    val thrown = the[ExecutionException] thrownBy {
      recoveredListenableFuture.get
    }
    thrown.getCause shouldBe recoverFail
  }

  test("ScalaFuture.recoverWith should recoverWith ListenableFuture") {
    val gFuture: ListenableFuture[Int] = Futures.immediateFailedFuture(new Exception)
    val sFuture = gFuture.asScala

    val recoveredFuture = sFuture.recoverWith { case ex: Exception => Future.successful(3) }
    recoveredFuture.await shouldBe 3
    recoveredFuture should not be a [ListenableFutureAdapter[_]]
    val recoveredListenableFuture = recoveredFuture.asListenableFuture
    recoveredListenableFuture.get shouldBe 3
  }

  test("ScalaFuture.recoverWith should throw same Exception in ListenableFuture") {
    val gFuture: ListenableFuture[Int] = Futures.immediateFailedFuture(new SomeException)
    val sFuture = gFuture.asScala

    val recoverFail = new SomeOtherException
    val recoveredFuture = sFuture.recoverWith { case ex: SomeException => Future.failed(recoverFail) }
    a [SomeOtherException] should be thrownBy recoveredFuture.await
    recoveredFuture should not be a [ListenableFutureAdapter[_]]
    val recoveredListenableFuture = recoveredFuture.asListenableFuture
    val thrown = the[ExecutionException] thrownBy {
      recoveredListenableFuture.get
    }
    thrown.getCause shouldBe recoverFail
  }

  test("ScalaFuture.recoverWith should throw same Exception in ListenableFuture when the recover pf blows") {
    val gFuture: ListenableFuture[Int] = Futures.immediateFailedFuture(new SomeException)
    val sFuture = gFuture.asScala

    val recoverFail = new SomeOtherException
    val recoveredFuture = sFuture.recoverWith { case ex: SomeException => throw recoverFail }
    a [SomeOtherException] should be thrownBy recoveredFuture.await
    recoveredFuture should not be a [ListenableFutureAdapter[_]]
    val recoveredListenableFuture = recoveredFuture.asListenableFuture
    val thrown = the[ExecutionException] thrownBy {
      recoveredListenableFuture.get
    }
    thrown.getCause shouldBe recoverFail
  }
}
