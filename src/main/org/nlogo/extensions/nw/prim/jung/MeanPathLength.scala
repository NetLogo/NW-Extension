// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext

class MeanPathLength(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val g = getGraphContext(context.getAgent.world).asJungGraph
    g.meanLinkPathLength(g.unweightedDijkstraShortestPath)
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}

class MeanWeightedPathLength(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(StringType),
    NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val weightVariable = args(0).getString.toUpperCase
    val g = getGraphContext(context.getAgent.world).asJungGraph
    g.meanLinkPathLength(g.weightedDijkstraShortestPath(weightVariable))
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}
