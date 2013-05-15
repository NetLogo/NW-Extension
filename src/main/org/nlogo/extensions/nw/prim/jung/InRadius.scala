// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.agent
import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext

import edu.uci.ics.jung.algorithms.filters.KNeighborhoodFilter

abstract class InRadiusPrim extends api.DefaultReporter {
  val edgeType: KNeighborhoodFilter.EdgeType
  val getGraphContext: api.World => GraphContext
  override def getSyntax = reporterSyntax(
    Array(NumberType),
    TurtlesetType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = getGraphContext(context.getAgent.world).asJungGraph
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val radius = args(0).getIntValue
    if (radius < 0) throw new api.ExtensionException("radius cannot be negative")
    graph.kNeighborhood(source, radius, edgeType)
  }
}

class TurtlesInRadius(
  override val getGraphContext: api.World => GraphContext)
  extends InRadiusPrim {
  override val edgeType = KNeighborhoodFilter.EdgeType.IN_OUT
}

class TurtlesInInRadius(
  override val getGraphContext: api.World => GraphContext)
  extends InRadiusPrim {
  override val edgeType = KNeighborhoodFilter.EdgeType.IN
}

class TurtlesInOutRadius(
  override val getGraphContext: api.World => GraphContext)
  extends InRadiusPrim {
  override val edgeType = KNeighborhoodFilter.EdgeType.OUT
}
