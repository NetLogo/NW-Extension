package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.{AgentSet, ExtensionException, ScalaConversions}
import org.nlogo.core.Syntax._
import org.nlogo.core.{AgentKind, LogoList}
import org.nlogo.agent
import org.nlogo.agent.Turtle
import org.nlogo.extensions.nw.{GraphContext, GraphContextProvider}
import org.nlogo.extensions.nw.algorithms.{ClusteringMetrics, Louvain}

import collection.JavaConverters._
import org.nlogo.api.TypeNames
import org.nlogo.extensions.nw.util.TurtleSetsConverters.toTurtleSet

class ClusteringCoefficient(gcp: GraphContextProvider) extends api.Reporter {
  override def getSyntax = reporterSyntax(ret = NumberType, agentClassString = "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world)
    ClusteringMetrics.clusteringCoefficient(graph, context.getAgent.asInstanceOf[agent.Turtle]): java.lang.Double
  }
}

class Modularity(gcp: GraphContextProvider) extends api.Reporter {
  override def getSyntax = reporterSyntax(right = List(ListType), ret = NumberType)
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world)
    val communities: Iterable[Set[Turtle]] = args(0).getList.toVector.map {
      case set: AgentSet if set.kind == AgentKind.Turtle =>
        set.agents.asScala.map(_.asInstanceOf[Turtle]).toSet
      case x: AnyRef => throw new ExtensionException(
        s"Expected the items of this list to be turtlesets, but got a ${TypeNames.name(x)}."
      )
    }
    ScalaConversions.toLogoObject(ClusteringMetrics.modularity(graph, communities))
  }
}

class LouvainCommunities(gcp: GraphContextProvider) extends api.Reporter {
  override def getSyntax = reporterSyntax(ret = ListType)
  override def report(args: Array[api.Argument], context: api.Context): LogoList = {
    val graph = gcp.getGraphContext(context.getAgent.world)
    ScalaConversions.toLogoList(Louvain.cluster(graph, context.getRNG).map(toTurtleSet))
  }
}


















