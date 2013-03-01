// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.ScalaConversions.toLogoObject
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetLogoGraph
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleToNetLogoTurtle

class DistanceTo(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[api.Turtle]
    val target = args(0).getAgent.asInstanceOf[api.Turtle]
    val graph = getGraph(context).asJungGraph
    val distance = Option(graph.dijkstraShortestPath.getDistance(source, target))
    toLogoObject(distance.getOrElse(false))
  }
}

class WeightedDistanceTo(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, StringType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[api.Turtle]
    val target = args(0).getAgent.asInstanceOf[api.Turtle]
    val weightVariable = args(1).getString.toUpperCase
    val graph = getGraph(context).asJungGraph
    val distance = Option(graph.dijkstraShortestPath(weightVariable).getDistance(source, target))
    toLogoObject(distance.getOrElse(false))
  }
}
