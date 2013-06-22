// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import java.util.Locale

import scala.collection.JavaConverters._

import org.nlogo.agent
import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext

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

class TurtlesOnWeightedPathTo(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, StringType),
    ListType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.asInstanceOf[agent.Turtle]

    val path =
      if (source == target)
        Vector(source)
      else {
        val graph = getGraphContext(context.getAgent.world).asJungGraph
        val weightVariable = args(1).getString.toUpperCase(Locale.ENGLISH)
        val linkPath = graph
          .weightedDijkstraShortestPath(weightVariable)
          .getPath(source, target)
        Distance.linkPathToTurtlePath(source, linkPath)
      }
    api.LogoList.fromVector(path)
  }
}

class WeightedPathTo(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, StringType),
    NumberType | BooleanType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val target = args(0).getAgent.asInstanceOf[agent.Turtle]
    val weightVariable = args(1).getString.toUpperCase(Locale.ENGLISH)
    val graph = getGraphContext(context.getAgent.world).asJungGraph
    api.LogoList.fromJava(
      graph
        .weightedDijkstraShortestPath(weightVariable)
        .getPath(source, target))
  }
}
