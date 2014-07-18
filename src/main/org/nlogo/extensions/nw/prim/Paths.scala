// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.agent
import org.nlogo.api
import org.nlogo.api.LogoList
import org.nlogo.api.ScalaConversions.toLogoObject
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentToRichAgent
import java.util.Locale
import org.nlogo.extensions.nw.GraphContextProvider
import org.nlogo.extensions.nw.GraphContextProvider
import org.nlogo.extensions.nw.GraphContextProvider
import org.nlogo.extensions.nw.GraphContextProvider
import org.nlogo.extensions.nw.GraphContextProvider
import org.nlogo.extensions.nw.GraphContextProvider

class DistanceTo(gcp: GraphContextProvider)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.requireAlive.asInstanceOf[agent.Turtle]
    val graphContext = gcp.getGraphContext(context.getAgent.world)
    toLogoObject(graphContext.distance(source, target).getOrElse(false))
  }
}

class WeightedDistanceTo(gcp: GraphContextProvider)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, StringType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.asInstanceOf[agent.Turtle]
    val weightVariable = args(1).getString.toUpperCase(Locale.ENGLISH)
    val distance = gcp.getGraphContext(context.getAgent.world).distance(source, target, Some(weightVariable))
    toLogoObject(distance.getOrElse(false))
  }
}

class PathTo(gcp: GraphContextProvider)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    ListType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.requireAlive.asInstanceOf[agent.Turtle]
    val graphContext = gcp.getGraphContext(context.getAgent.world)
    def turtlesToLinks(turtles: List[agent.Turtle]): Iterator[agent.Link] =
      for {
        (source, target) <- turtles.iterator zip turtles.tail.iterator
        l = graphContext
          .edges(source, true, false, true)
          .filter(l => l.end1 == target || l.end2 == target)
          .head
      } yield l
    graphContext.path(source, target)
      .map { p => LogoList.fromIterator(turtlesToLinks(p.toList)) }
      .getOrElse(LogoList.Empty)
  }
}

class TurtlesOnPathTo(gcp: GraphContextProvider)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    ListType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.requireAlive.asInstanceOf[agent.Turtle]
    val graphContext = gcp.getGraphContext(context.getAgent.world)
    graphContext.path(source, target)
      .map { p => LogoList.fromIterator(p.iterator) }
      .getOrElse(LogoList.Empty)
  }
}

class TurtlesOnWeightedPathTo(gcp:GraphContextProvider)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, StringType),
    ListType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.asInstanceOf[agent.Turtle]
    val weightVariable = args(1).getString.toUpperCase(Locale.ENGLISH)
    val graphContext = gcp.getGraphContext(context.getAgent.world)
    graphContext.path(source, target, Some(weightVariable))
      .map { p => LogoList.fromIterator(p.iterator) }
      .getOrElse(LogoList.Empty)
  }
}

class WeightedPathTo(gcp: GraphContextProvider)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, StringType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.asInstanceOf[agent.Turtle]
    val weightVariable = args(1).getString.toUpperCase(Locale.ENGLISH)
    val graphContext = gcp.getGraphContext(context.getAgent.world)
    def turtlesToLinks(turtles: List[agent.Turtle]): Iterator[agent.Link] =
      for {
        (source, target) <- turtles.iterator zip turtles.tail.iterator
        l = graphContext
          .edges(source, true, false, true)
          .filter(l => l.end1 == target || l.end2 == target)
          .head
      } yield l
    graphContext
      .path(source, target, Some(weightVariable))
      .map { p => LogoList.fromIterator(turtlesToLinks(p.toList)) }
      .getOrElse(LogoList.Empty)
  }
}
