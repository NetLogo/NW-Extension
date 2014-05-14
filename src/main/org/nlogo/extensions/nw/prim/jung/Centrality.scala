// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.agent
import java.util.Locale

class BetweennessCentrality(getGraphContext: api.World => GraphContext) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType, "-T-L")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = getGraphContext(context.getAgent.world).asJungGraph
    Double.box(graph
      .BetweennessCentrality
      .get(context.getAgent.asInstanceOf[agent.Turtle]))
  }
}

class EigenvectorCentrality(getGraphContext: api.World => GraphContext) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType, "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = getGraphContext(context.getAgent.world).asUndirectedJungGraph
    // make sure graph is connected
    if (graph.isWeaklyConnected) // TODO: Actually, it should be STRONGLY connected
      graph.gc.eigenvectorCentrality(context.getAgent.asInstanceOf[agent.Turtle]).asInstanceOf[java.lang.Double]
    else
      java.lang.Boolean.FALSE
  }
}

class ClosenessCentrality(getGraphContext: api.World => GraphContext) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType, "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = getGraphContext(context.getAgent.world).asJungGraph
    graph.closenessCentrality(context.getAgent.asInstanceOf[agent.Turtle]): java.lang.Double
  }
}

class WeightedClosenessCentrality(getGraphContext: api.World => GraphContext) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(Array(StringType), NumberType, "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = getGraphContext(context.getAgent.world).asJungGraph
    val varName = args(0).getString.toUpperCase(Locale.ENGLISH)
    graph.closenessCentrality(context.getAgent.asInstanceOf[agent.Turtle], varName): java.lang.Double
  }
}
