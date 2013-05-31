// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.algorithms

import org.nlogo.agent
import org.nlogo.extensions.nw.GraphContext

object Distance {
  def distance(
    graphContext: GraphContext,
    source: agent.Turtle,
    target: agent.Turtle): Option[Int] =
    new BreadthFirstSearch(graphContext)
      .from(source, graphContext.isDirected)
      .find(_.head eq target)
      .map(_.size - 1)
}
