// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import scala.collection.JavaConverters._

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetLogoGraph
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleToNetLogoTurtle

object Distance {
  // This will shortly become a primitive of it's own...
  def linkPathToTurtlePath(
    source: org.nlogo.api.Turtle,
    linkPath: java.util.List[org.nlogo.agent.Link]) =
    if (linkPath.isEmpty)
      Vector()
    else
      linkPath.asScala.foldLeft(Vector(source)) {
        case (turtles, link) =>
          turtles :+ (if (link.end1 != turtles.last) link.end1 else link.end2)
      }
}

class PathTo(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    ListType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[api.Turtle]
    val target = args(0).getAgent.asInstanceOf[api.Turtle]
    api.LogoList.fromJava(
      getGraph(context).asJungGraph
        .dijkstraShortestPath
        .getPath(source, target))
  }
}

class TurtlesOnPathTo(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType),
    ListType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[api.Turtle]
    val target = args(0).getAgent.asInstanceOf[api.Turtle]
    val path =
      if (source == target)
        Vector(source)
      else {
        val graph = getGraph(context).asJungGraph
        val linkPath = graph.dijkstraShortestPath.getPath(source, target)
        Distance.linkPathToTurtlePath(source, linkPath)
      }
    api.LogoList.fromVector(path)
  }
}

class TurtlesOnWeightedPathTo(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, StringType),
    ListType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[api.Turtle]
    val target = args(0).getAgent.asInstanceOf[api.Turtle]

    val path =
      if (source == target)
        Vector(source)
      else {
        val graph = getGraph(context).asJungGraph
        val weightVariable = args(1).getString.toUpperCase
        val linkPath = graph.dijkstraShortestPath(weightVariable).getPath(source, target)
        Distance.linkPathToTurtlePath(source, linkPath)
      }
    api.LogoList.fromVector(path)
  }
}

class WeightedPathTo(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, StringType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[api.Turtle]
    val target = args(0).getAgent.asInstanceOf[api.Turtle]
    val weightVariable = args(1).getString.toUpperCase
    api.LogoList.fromJava(
      getGraph(context).asJungGraph
        .dijkstraShortestPath(weightVariable)
        .getPath(source, target))
  }
}