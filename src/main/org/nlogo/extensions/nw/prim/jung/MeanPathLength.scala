// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetLogoGraph

class MeanPathLength(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    getGraph(context).asJungGraph
      .dijkstraShortestPath
      .meanLinkPathLength
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}

class MeanWeightedPathLength(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(StringType),
    NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    getGraph(context).asJungGraph
      .dijkstraShortestPath(args(0).getString.toUpperCase)
      .meanLinkPathLength
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}