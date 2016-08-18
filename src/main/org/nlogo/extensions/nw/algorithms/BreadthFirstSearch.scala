// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.algorithms

import org.nlogo.extensions.nw.GraphContext
import org.nlogo.agent.Turtle

object BreadthFirstSearch {
  /**
   * Traverses the network in breadth-first order.
   * Each List[Turtle] is a reversed path (destination is head);
   * the paths share storage, so total memory usage stays within O(n).
   * Adapted from the original network extension written by Seth Tisue
   */
  def apply(gc: GraphContext, start: Turtle, reverse: Boolean = false): Stream[List[Turtle]] = {
    val rawNeighbors = if (reverse) (gc.inNeighbors _) else (gc.outNeighbors _)
    val seen: Turtle => Boolean = {
      val memory = collection.mutable.HashSet[Turtle](start)
      t => memory(t) || { memory += t; false }
    }
    def neighbors(turtle: Turtle): Iterable[Turtle] = rawNeighbors(turtle).filterNot(seen)
    def nextLayer(layer: Stream[List[Turtle]]) =
      for {
        path <- layer
        neighbor <- neighbors(path.head)
      } yield neighbor :: path
    Stream.iterate(Stream(List(start)))(nextLayer)
      .takeWhile(_.nonEmpty)
      .flatten
  }
}
