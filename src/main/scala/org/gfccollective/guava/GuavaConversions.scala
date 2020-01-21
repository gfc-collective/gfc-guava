package org.gfccollective.guava

import com.google.common.base.{Optional, Predicate => GuavaPredicate, Supplier, Function => GFunction}

/**
 * In spirit of scala.collection.JavaConversions.
 * Implicit conversion functions for guava package.
 */
object GuavaConversions {

  import scala.language.implicitConversions

  /** Implicit conversion from guava's <code>Optional</code> to
   *  to scala's <code>Option</code>
   */
  implicit def asScalaOption[T](gOpt: Optional[T]): Option[T] = {
    if (gOpt.isPresent) Some(gOpt.get)
    else None
  }

  /** Implicit conversion from scala's <code>Option</code> to
   *  to guava's <code>Optional</code>
   */
  implicit def asJavaOptional[T](sOpt: Option[T]): Optional[T] = {
    sOpt.map(Optional.of[T]).getOrElse(Optional.absent())
  }

  implicit def asJavaFunction[T, R](f: T => R): GFunction[T, R] =
    new GFunction[T, R] {
      def apply(arg: T): R = f(arg)
    }

  implicit def asScalaFunction[T, R](f: GFunction[T, R]): (T => R) = { f.apply(_) }

  implicit def asJavaSupplier[R](s: () => R): Supplier[R] =
    new Supplier[R] {
      def get(): R = s()
    }

  implicit def asScalaFunction0[R](s: Supplier[R]): (() => R) = { s.get _ }

  implicit def asScalaPredicate[T](pred: GuavaPredicate[T]): Predicate[T] = Predicate(pred)

  implicit def asJavaPredicate[T](pred: T => Boolean): Predicate[T] = Predicate(pred)

  /**
   * Can't implicitly convert by-name params, so this method needs to be invoked
   * explicitly.
   */
  def supplier[T](f: => T): Supplier[T] = asJavaSupplier { () => f }
}
