package com.gilt.gfc.guava

import com.google.common.base.{Preconditions => GPreconditions}
/**
 * Preconditions wrapper for use from scala.
 *
 * We want to enforce the use of a status message, hence the argument only methods are not exposed.
 *
 * @author Gregor Heine
 * @since 11/Apr/2012 15:10
 */
object Preconditions {
  def checkNotNull[T](ref: T, msg: String, args: Any*): T = GPreconditions.checkNotNull(ref, msg, args)

  def checkArgument(expr: Boolean, msg: String, args: Any*) { GPreconditions.checkArgument(expr, msg, args) }

  def checkState(expr: Boolean, msg: String, args: Any*) { GPreconditions.checkState(expr, msg, args) }
}
