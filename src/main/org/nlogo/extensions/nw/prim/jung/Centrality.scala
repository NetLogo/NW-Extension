// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetLogoGraph
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentToNetLogoAgent
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleToNetLogoTurtle

class BetweennessCentrality(getGraph: api.Context => NetLogoGraph) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType, "-T-L")
  override def report(args: Array[api.Argument], context: api.Context) =
    Double.box(getGraph(context).asJungGraph
      .BetweennessCentrality
      .get(context.getAgent))
}

class EigenvectorCentrality(getGraph: api.Context => NetLogoGraph) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType, "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val g = getGraph(context).asUndirectedJungGraph
    // make sure graph is connected
    if (g.isWeaklyConnected) // TODO: Actually, it should be STRONGLY connected
      g.EigenvectorCentrality
        .getScore(TurtleToNetLogoTurtle(context.getAgent.asInstanceOf[api.Turtle]))
    else
      java.lang.Boolean.FALSE
  }
}

class ClosenessCentrality(getGraph: api.Context => NetLogoGraph) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType, "-T--")
  override def report(args: Array[api.Argument], context: api.Context) =
    getGraph(context).asJungGraph
      .ClosenessCentrality
      .getScore(context.getAgent.asInstanceOf[api.Turtle])
}