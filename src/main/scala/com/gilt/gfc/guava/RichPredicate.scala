package com.gilt.gfc.guava

import com.google.common.base.{Predicate, Predicates}

/**
 * Wraps Guava predicates with some simple methods that make it easier use in Scala.
 *
 * @param self The predicate to proxy.
 * @tparam T The type of the proxied predicate.
 *
 * @author Eric Czarny
 */
class RichPredicate[T](val self: Predicate[T]) extends Proxy {

  /**
   * Returns a predicate that evaluates to true if this and the given predicate evaluate to true.
   *
   * @param rhs The predicate on the right hand side of this predicate.
   *
   * @return A new predicate that evaluates to true if the necessary conditions are met.
   */
  def &&(rhs: Predicate[T]) = Predicates.and[T](self, rhs)

  /**
   * Returns a predicate that evaluates to true if this or the given predicate evaluate to true.
   *
   * @param rhs The predicate on the right hand side of this predicate.
   *
   * @return A new predicate that evaluates to true if the necessary conditions are met.
   */
  def ||(rhs: Predicate[T]) = Predicates.or[T](self, rhs)

  /**
   * Returns a predicate that evaluates to true if this predicate evaluates to false.
   *
   * @return A new predicate that evaluates to true if the necessary conditions are met.
   */
  def unary_!() = Predicates.not[T](self)
}
