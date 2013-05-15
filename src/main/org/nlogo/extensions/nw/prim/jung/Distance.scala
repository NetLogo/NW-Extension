// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.agent
import org.nlogo.api
import org.nlogo.api.ScalaConversions.toLogoObject
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext

class DistanceTo(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.asInstanceOf[agent.Turtle]
    val graph = getGraphContext(context.getAgent.world).asJungGraph
    val distance = Option(graph.dijkstraShortestPath.getDistance(source, target))
    toLogoObject(distance.getOrElse(false))
  }
}

class WeightedDistanceTo(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, StringType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.asInstanceOf[agent.Turtle]
    val weightVariable = args(1).getString.toUpperCase
    val graph = getGraphContext(context.getAgent.world).asJungGraph
    val distance = Option(graph.dijkstraShortestPath(weightVariable).getDistance(source, target))
    toLogoObject(distance.getOrElse(false))
  }
}
