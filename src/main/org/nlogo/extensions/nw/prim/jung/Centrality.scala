// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.core.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.agent
import java.util.Locale
import org.nlogo.agent.Agent
import org.nlogo.extensions.nw.GraphContextProvider

class BetweennessCentrality(gcp:GraphContextProvider) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ret = NumberType, agentClassString = "-T-L")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world).asJungGraph
    graph.betweennessCentrality(context.getAgent.asInstanceOf[Agent]): java.lang.Double
  }
}

class WeightedBetweennessCentrality(gcp: GraphContextProvider) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(right = List(StringType), ret = NumberType, agentClassString = "-T-L")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world).asJungGraph
    val weightVar = args(0).getString.toUpperCase(Locale.ENGLISH)
    graph.betweennessCentrality(context.getAgent.asInstanceOf[Agent], weightVar): java.lang.Double
  }
}

class EigenvectorCentrality(gcp: GraphContextProvider) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ret = NumberType, agentClassString = "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world).asUndirectedJungGraph
    // make sure graph is connected
    if (graph.isWeaklyConnected) // TODO: Actually, it should be STRONGLY connected
      graph.gc.eigenvectorCentrality(context.getAgent.asInstanceOf[agent.Turtle]).asInstanceOf[java.lang.Double]
    else
      java.lang.Boolean.FALSE
  }
}

class PageRank(gcp: GraphContextProvider) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ret = NumberType, agentClassString = "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world).asUndirectedJungGraph
    graph.PageRank.getScore(context.getAgent.asInstanceOf[agent.Turtle]).asInstanceOf[java.lang.Double]
  }
}

class ClosenessCentrality(gcp: GraphContextProvider) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ret = NumberType, agentClassString = "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world).asJungGraph
    graph.closenessCentrality(context.getAgent.asInstanceOf[agent.Turtle]): java.lang.Double
  }
}

class WeightedClosenessCentrality(gcp: GraphContextProvider) extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(right = List(StringType), ret = NumberType, agentClassString = "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world).asJungGraph
    val varName = args(0).getString.toUpperCase(Locale.ENGLISH)
    graph.closenessCentrality(context.getAgent.asInstanceOf[agent.Turtle], varName): java.lang.Double
  }
}
