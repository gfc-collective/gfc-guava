package com.gilt.gfc.guava

import java.lang.{Iterable => JIterable}
import scala.collection.JavaConverters._
import com.google.common.collect.Range
import com.google.common.base.{Function, Optional}

/**
 * Helper to convert Ranges to Option tuples [lower-bound, upper-bound) and vice-versa
 *
 * @author Gregor Heine
 * @since 03/Dec/2013 18:30
 */
object RangeHelper {
  def build[T <: Comparable[_]](lower: Option[T], upper:  Option[T]): Option[Range[T]] = {
    (lower, upper) match {
      case (Some(lower), Some(upper)) if (lower == upper) => Some(Range.singleton(lower))
      case (Some(lower), Some(upper))                     => Some(Range.closedOpen(lower, upper))
      case (Some(lower), None)                            => Some(Range.atLeast(lower))
      case (None, Some(upper))                            => Some(Range.lessThan(upper))
      case (None, None)                                   => None
    }
  }

  def buildf[S, T <: Comparable[_]](lower: Option[S], upper: Option[S])(f: S => T): Option[Range[T]] = {
    build(lower.map(f), upper.map(f))
  }

  def decompose[T <: Comparable[_]](range: Option[Range[T]]): (Option[T], Option[T]) = {
    range.map { range =>
      val lower = if (range.hasLowerBound) {
        Some(range.lowerEndpoint)
      } else {
        None
      }
      val upper = if (range.hasUpperBound) {
        Some(range.upperEndpoint)
      } else {
        None
      }
      (lower, upper)
    }.getOrElse((None, None))
  }

  def decomposef[S <: Comparable[_], T](range: Option[Range[S]])(f: S => T): (Option[T], Option[T]) = {
    val (lower, upper) = decompose(range)
    (lower.map(f), upper.map(f))
  }

  def decomposeStr[T <: Comparable[_]](range: Option[Range[T]]): (Option[String], Option[String]) = decomposef(range)(_.toString)

  def span[T <: Comparable[_]](ranges: Iterable[Range[T]]): Option[Range[T]] = {
    ranges.foldLeft(None: Option[Range[T]]) { (accRange, range) =>
      (accRange, range) match {
        case (Some(r1), r2) => Some(r1.span(r2))
        case _ => Some(range)
      }
    }
  }
}

object JRangeHelper {
  import com.gilt.gfc.guava.GuavaConverters._

  def build[T <: Comparable[_]](lower: Optional[T], upper:  Optional[T]): Optional[Range[T]] =
    RangeHelper.build(lower.asScala, upper.asScala).asJava

  def buildf[S, T <: Comparable[_]](lower: Optional[S], upper: Optional[S], func: Function[S, T]): Optional[Range[T]] =
    RangeHelper.buildf(lower.asScala, upper.asScala)(func.asScala).asJava

  def decompose[T <: Comparable[_]](range: Optional[Range[T]]): Array[Optional[T]] =
    tupleToArray(RangeHelper.decompose(range.asScala))

  def decomposef[S <: Comparable[_], T](range: Optional[Range[S]], func: Function[S, T]): Array[Optional[T]] =
    tupleToArray(RangeHelper.decomposef(range.asScala)(func.asScala))

  def decomposeStr[T <: Comparable[_]](range: Optional[Range[T]]): Array[Optional[String]] =
    tupleToArray(RangeHelper.decomposeStr(range.asScala))

  def span[T <: Comparable[_]](ranges: JIterable[Range[T]]): Optional[Range[T]] =
    RangeHelper.span(ranges.asScala).asJava

  private def tupleToArray[T](t: (Option[T], Option[T])): Array[Optional[T]] = Array(t._1.asJava, t._2.asJava)
}
