// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import java.util.Locale

import org.nlogo.agent.Turtle
import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.algorithms.MeanPathLength.meanPathLength

class MeanWeightedPathLength(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(StringType),
    NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val weightVariable = args(0).getString.toUpperCase(Locale.ENGLISH)
    val gc = getGraphContext(context.getAgent.world)
    val dist = (source: Turtle, target: Turtle) =>
      Option(gc.asJungGraph.weightedDijkstraShortestPath(weightVariable)
        .getDistance(source, target))
        .map(_.doubleValue())
    meanPathLength(gc.turtles, dist)
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}
