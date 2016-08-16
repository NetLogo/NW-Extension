package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.{AgentSet, ExtensionException, ScalaConversions}
import org.nlogo.core.Syntax._
import org.nlogo.core.{AgentKind}
import org.nlogo.agent
import org.nlogo.agent.Turtle
import org.nlogo.extensions.nw.{GraphContext, GraphContextProvider}
import collection.JavaConverters._
import org.nlogo.api.TypeNames

class ClusteringCoefficient(gcp: GraphContextProvider) extends api.Reporter {
  override def getSyntax = reporterSyntax(ret = NumberType, agentClassString = "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world)
    graph.clusteringCoefficient(context.getAgent.asInstanceOf[agent.Turtle]): java.lang.Double
  }
}

class Modularity(gcp: GraphContextProvider) extends api.Reporter {
  override def getSyntax = reporterSyntax(right = List(ListType), ret = NumberType)
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world)
    val communities: Iterable[Iterable[Turtle]] = args(0).getList.toVector.map {
      case set: AgentSet if set.kind == AgentKind.Turtle =>
        set.agents.asScala.map(_.asInstanceOf[Turtle])
      case x: AnyRef => throw new ExtensionException(
        s"Expected the items of this list to be turtlesets, but got a ${TypeNames.name(x)}."
      )
    }
    ScalaConversions.toLogoObject(graph.modularity(communities))
  }
}


















