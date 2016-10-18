package com.gilt.gfc.guava

import com.google.common.base.{Predicate => GuavaPredicate}

/**
 * An attempt to combine scala and guava predicates
 *
 * @author Gregor Heine
 * @since 09/Dec/2014 11:44
 */
trait Predicate[T] extends (T => Boolean) with GuavaPredicate[T] {
  /**
   * Returns a predicate that evaluates to true if this and the given predicate evaluate to true.
   *
   * @param rhs The predicate on the right hand side of this predicate.
   *
   * @return A new predicate that evaluates to true if the necessary conditions are met.
   */
  def &&(rhs: Predicate[T]) = Predicate.and[T](this, rhs)

  /**
   * Returns a predicate that evaluates to true if this or the given predicate evaluate to true.
   *
   * @param rhs The predicate on the right hand side of this predicate.
   *
   * @return A new predicate that evaluates to true if the necessary conditions are met.
   */
  def ||(rhs: Predicate[T]) = Predicate.or[T](this, rhs)

  /**
   * Returns a predicate that evaluates to true if this predicate evaluates to false.
   *
   * @return A new predicate that evaluates to true if the necessary conditions are met.
   */
  def unary_!() = Predicate.not[T](this)
}

object Predicate {
  def apply[T](f: T => Boolean): Predicate[T] = new Predicate[T] {
    override def apply(t: T): Boolean = f(t)
  }

  def apply[T](f: GuavaPredicate[T]): Predicate[T] = new Predicate[T] {
    override def apply(t: T): Boolean = f(t)
  }

  def and[T](p: Predicate[T]*): Predicate[T] = new Predicate[T] {
    override def apply(t: T): Boolean = p.forall(_(t))
  }

  def or[T](p: Predicate[T]*): Predicate[T] = new Predicate[T] {
    override def apply(t: T): Boolean = p.exists(_(t))
  }

  def not[T](p: Predicate[T]): Predicate[T] = new Predicate[T] {
    override def apply(t: T): Boolean = !p(t)
  }

  val True: Predicate[Any] = Predicate((_: Any) => true)
  def alwaysTrue[T <: Any]: Predicate[T] = True.asInstanceOf[Predicate[T]]

  val False: Predicate[Any] = Predicate((_: Any) => false)
  def alwaysFalse[T]: Predicate[T] = False.asInstanceOf[Predicate[T]]
}
