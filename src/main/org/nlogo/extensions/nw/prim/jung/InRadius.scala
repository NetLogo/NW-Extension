// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetLogoGraph
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleToNetLogoTurtle

import edu.uci.ics.jung.algorithms.filters.KNeighborhoodFilter

abstract class InRadiusPrim extends api.DefaultReporter {
  val edgeType: KNeighborhoodFilter.EdgeType
  val getGraph: api.Context => NetLogoGraph
  override def getSyntax = reporterSyntax(
    Array(NumberType),
    TurtlesetType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = getGraph(context).asJungGraph
    val source = context.getAgent.asInstanceOf[api.Turtle]
    val radius = args(0).getIntValue
    if (radius < 0) throw new api.ExtensionException("radius cannot be negative")
    graph.kNeighborhood(source, radius, edgeType)
  }
}

class TurtlesInRadius(
  override val getGraph: api.Context => NetLogoGraph)
  extends InRadiusPrim {
  override val edgeType = KNeighborhoodFilter.EdgeType.IN_OUT
}

class TurtlesInInRadius(
  override val getGraph: api.Context => NetLogoGraph)
  extends InRadiusPrim {
  override val edgeType = KNeighborhoodFilter.EdgeType.IN
}

class TurtlesInOutRadius(
  override val getGraph: api.Context => NetLogoGraph)
  extends InRadiusPrim {
  override val edgeType = KNeighborhoodFilter.EdgeType.OUT
}
