// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.agent
import org.nlogo.api
import org.nlogo.api.LogoList
import org.nlogo.api.ScalaConversions.toLogoObject
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentToRichAgent
import org.nlogo.extensions.nw.algorithms.BreadthFirstSearch
import org.nlogo.extensions.nw.algorithms.Distance.distance

class DistanceTo(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.requireAlive.asInstanceOf[agent.Turtle]
    val graphContext = getGraphContext(context.getAgent.world)
    toLogoObject(distance(graphContext, source, target).getOrElse(false))
  }
}

class PathTo(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    ListType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.requireAlive.asInstanceOf[agent.Turtle]
    val graphContext = getGraphContext(context.getAgent.world)
    def turtlesToLinks(turtles: List[agent.Turtle]): Iterator[agent.Link] =
      for {
        (source, target) <- turtles.iterator zip turtles.tail.iterator
        l = graphContext
          .edges(source, true, false, true)
          .filter(l => l.end1 == target || l.end2 == target)
          .head
      } yield l
    new BreadthFirstSearch(graphContext)
      .from(source, true, false, true)
      .find(_.head eq target)
      .map(path => LogoList.fromIterator(turtlesToLinks(path.reverse)))
      .getOrElse(LogoList.Empty)
  }
}

class TurtlesOnPathTo(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    ListType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.requireAlive.asInstanceOf[agent.Turtle]
    val graphContext = getGraphContext(context.getAgent.world)
    new BreadthFirstSearch(graphContext)
      .from(source, true, false, true)
      .find(_.head eq target)
      .map(path => LogoList.fromIterator(path.reverseIterator))
      .getOrElse(LogoList.Empty)
  }
}
