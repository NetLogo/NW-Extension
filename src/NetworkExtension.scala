package org.nlogo.extensions.nw

import org.nlogo.api.Turtle
import org.nlogo.api.Link
import org.nlogo.api.Agent
import org.nlogo.api.AgentSet
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.DefaultClassManager
import org.nlogo.api.DefaultReporter
import org.nlogo.api.ExtensionException
import org.nlogo.api.I18N
import org.nlogo.api.PrimitiveManager
import org.nlogo.api.Syntax
import org.nlogo.{ agent => nla }

class NetworkExtension extends DefaultClassManager {
  override def load(primManager: PrimitiveManager) {
    primManager.addPrimitive("link-distance-s", LinkDistanceStatic)
    primManager.addPrimitive("link-distance-l", LinkDistanceLive)
    primManager.addPrimitive("link-path-s", LinkPathStatic)
    primManager.addPrimitive("link-path-l", LinkPathLive)
    primManager.addPrimitive("snapshot", Snapshot)
  }
}

trait Helpers {
  def requireTurtleSet(agents: AgentSet) {
    if (!classOf[Turtle].isAssignableFrom(agents.`type`))
      throw new ExtensionException("Expected input to be a turtleset")
  }
  def requireLinkSet(agents: AgentSet) {
    if (!classOf[Link].isAssignableFrom(agents.`type`))
      throw new ExtensionException("Expected input to be a linkset")
  }
  def requireLinkBreed(context: Context, agents: AgentSet, allowDirected: Boolean = true, allowUndirected: Boolean = true) {
    val world = context.getAgent.world
    if ((agents ne world.links) &&
      !world.asInstanceOf[org.nlogo.agent.World].isLinkBreed(agents.asInstanceOf[org.nlogo.agent.AgentSet]))
      throw new ExtensionException(
        I18N.errors.get("org.nlogo.prim.etc.$common.expectedLastInputToBeLinkBreed"))
  }
  def requireAlive(agent: Agent) {
    if (agent.id == -1)
      throw new ExtensionException(
        I18N.errors.get("org.nlogo.$common.thatAgentIsDead"))
  }
}

object Snapshot
  extends DefaultReporter {
  override def getSyntax =
    Syntax.reporterSyntax(
      Array(Syntax.LinksetType, Syntax.TurtlesetType),
      Syntax.WildcardType,
      agentClassString = "OTPL")
  override def report(args: Array[Argument], context: Context): AnyRef = {
    val linkSet = args(0).getAgentSet.asInstanceOf[nla.AgentSet]
    val turtleSet = args(1).getAgentSet.asInstanceOf[nla.AgentSet]
    new StaticJungGraph(linkSet, turtleSet)
  }
}
object LinkDistanceStatic
  extends DefaultReporter
  with Helpers {
  override def getSyntax =
    Syntax.reporterSyntax(
      Array(Syntax.TurtleType, Syntax.LinksetType, Syntax.TurtlesetType),
      Syntax.NumberType | Syntax.BooleanType,
      agentClassString = "-T--")
  override def report(args: Array[Argument], context: Context): AnyRef = {
    val destNode = args(0).getTurtle
    requireAlive(destNode)
    val start = context.getAgent.asInstanceOf[nla.Turtle]
    val end = destNode.asInstanceOf[nla.Turtle]
    val linkSet = args(1).getAgentSet.asInstanceOf[nla.AgentSet]
    val turtleSet = args(1).getAgentSet.asInstanceOf[nla.AgentSet]
    Metrics.linkDistanceJung(start, end, new StaticJungGraph(linkSet, turtleSet))
      .map(Double.box(_))
      .getOrElse(java.lang.Boolean.FALSE)

    val path = new StaticJungGraph(linkSet, turtleSet).dijkstraShortestPath.getPath(start, end)
    Option(path.size)
      .filterNot(0==)
      .map(Double.box(_))
      .getOrElse(java.lang.Boolean.FALSE)
  }
}

object LinkDistanceLive
  extends DefaultReporter
  with Helpers {
  override def getSyntax =
    Syntax.reporterSyntax(
      Array(Syntax.TurtleType, Syntax.LinksetType),
      Syntax.NumberType | Syntax.BooleanType,
      agentClassString = "-T--")
  override def report(args: Array[Argument], context: Context): AnyRef = {
    val destNode = args(0).getTurtle
    requireAlive(destNode)
    val start = context.getAgent.asInstanceOf[nla.Turtle]
    val end = destNode.asInstanceOf[nla.Turtle]
    val linkSet = args(1).getAgentSet.asInstanceOf[nla.AgentSet]

    val path = new LiveJungGraph(linkSet).dijkstraShortestPath.getPath(start, end)
    Option(path.size)
      .filterNot(0==)
      .map(Double.box(_))
      .getOrElse(java.lang.Boolean.FALSE)
  }
}

object LinkPathStatic
  extends DefaultReporter
  with Helpers {
  override def getSyntax =
    Syntax.reporterSyntax(
      Array(Syntax.TurtleType, Syntax.LinksetType, Syntax.TurtlesetType),
      Syntax.ListType,
      agentClassString = "-T--")
  override def report(args: Array[Argument], context: Context): AnyRef = {
    val destNode = args(0).getTurtle
    requireAlive(destNode)
    val start = context.getAgent.asInstanceOf[nla.Turtle]
    val end = destNode.asInstanceOf[nla.Turtle]
    val linkSet = args(1).getAgentSet.asInstanceOf[nla.AgentSet]
    val turtleSet = args(2).getAgentSet.asInstanceOf[nla.AgentSet]
    Metrics.linkPathJung(start, end, new StaticJungGraph(linkSet, turtleSet))
  }
}

object LinkPathLive
  extends DefaultReporter
  with Helpers {
  override def getSyntax =
    Syntax.reporterSyntax(
      Array(Syntax.TurtleType, Syntax.LinksetType),
      Syntax.ListType,
      agentClassString = "-T--")
  override def report(args: Array[Argument], context: Context): AnyRef = {
    val destNode = args(0).getTurtle
    requireAlive(destNode)
    val start = context.getAgent.asInstanceOf[nla.Turtle]
    val end = destNode.asInstanceOf[nla.Turtle]
    val linkSet = args(1).getAgentSet.asInstanceOf[nla.AgentSet]
    Metrics.linkPathJung(start, end, new LiveJungGraph(linkSet))
  }
}