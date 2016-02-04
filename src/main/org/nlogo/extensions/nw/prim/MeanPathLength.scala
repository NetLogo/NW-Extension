package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.core.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.algorithms.MeanPathLength._
import org.nlogo.agent.Turtle
import java.util.Locale
import org.nlogo.extensions.nw.GraphContextProvider
import org.nlogo.extensions.nw.GraphContextProvider

class MeanPathLength(gcp: GraphContextProvider)
  extends api.Reporter {
  override def getSyntax = reporterSyntax(ret = NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val gc = gcp.getGraphContext(context.getAgent.world)
    val dist = gc.distance(_: Turtle, _: Turtle)
    meanPathLength(gc.turtles, dist)
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}

class MeanWeightedPathLength(gcp: GraphContextProvider)
  extends api.Reporter {
  override def getSyntax = reporterSyntax(
    right = List(StringType),
    ret = NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val weightVariable = args(0).getString.toUpperCase(Locale.ENGLISH)
    val gc = gcp.getGraphContext(context.getAgent.world)
    val dist = gc.distance(_: Turtle, _: Turtle, Some(weightVariable))
    meanPathLength(gc.turtles, dist)
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}
