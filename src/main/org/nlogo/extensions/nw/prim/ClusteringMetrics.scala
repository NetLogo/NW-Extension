package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.core.Syntax._
import org.nlogo.agent
import org.nlogo.extensions.nw.{GraphContext, GraphContextProvider}

class ClusteringCoefficient(gcp: GraphContextProvider) extends api.Reporter {
  override def getSyntax = reporterSyntax(ret = NumberType, agentClassString = "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world)
    graph.clusteringCoefficient(context.getAgent.asInstanceOf[agent.Turtle]): java.lang.Double
  }
}



















