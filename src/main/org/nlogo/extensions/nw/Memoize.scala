package org.nlogo.extensions.nw
import collection.mutable

/**
 * A basic unary function memoizer.
 */
object Memoize {
  def apply[A,B](fn: A=>B): A=>B = {
    val cache = mutable.Map.empty[A,B]
    (x) => cache getOrElseUpdate(x, fn(x))
  }
}

