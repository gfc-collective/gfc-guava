package com.gilt.gfc.guava

import com.google.common.base.{Optional, Predicate => GuavaPredicate, Supplier, Function => GFunction}

/**
 * In spirit of scala.collection.JavaConverters.
 * Implicit conversion functions for guava package.
 */
object GuavaConverters {

  implicit class OptionConverter[T](val optional: Optional[T]) extends AnyVal {
    @inline def asScala: Option[T] = Option(optional.orNull())
  }
  implicit class OptionalConverter[T](val option: Option[T]) extends AnyVal {
    @inline def asJava: Optional[T] = option.map(Optional.of[T]).getOrElse(Optional.absent[T]())
  }

  implicit class ScalaFunctionConverter[F, T](f: F => T) {
    @inline def asJava = new GFunction[F, T] {
      override def apply(input: F): T = f(input)
      override def equals(obj: Any): Boolean = super.equals(obj)
    }
  }

  implicit class GuavaFunctionConverter[T, R](val f: GFunction[T, R]) extends AnyVal {
    @inline def asScala: T => R = (x: T) => f.apply(x)
  }

  implicit class ScalaSupplierConverter[R](s: () => R) {
    @inline def asJava: Supplier[R] = new Supplier[R] {
      override def get(): R = s()
    }
  }

  implicit class GuavaSupplierConverter[R](val s: Supplier[R]) extends AnyVal {
    @inline def asScala: () => R = () => s.get()
  }

  implicit class ScalaPredicateConverter[T](val pred: T => Boolean) {
    @inline def asJava: Predicate[T] = Predicate(pred)
  }

  implicit class GuavaPredicateConverter[T](val pred: GuavaPredicate[T]) extends AnyVal {
    @inline def asScala: Predicate[T] = Predicate(pred)
  }
}
