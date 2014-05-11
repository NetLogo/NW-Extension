package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.algorithms.MeanPathLength._
import org.nlogo.agent.Turtle
import java.util.Locale

class MeanPathLength(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val gc = getGraphContext(context.getAgent.world)
    val dist = gc.distance(_: Turtle, _: Turtle)
    meanPathLength(gc.turtles, dist)
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}

class MeanWeightedPathLength(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(StringType),
    NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val weightVariable = args(0).getString.toUpperCase(Locale.ENGLISH)
    val gc = getGraphContext(context.getAgent.world)
    val dist = gc.distance(_: Turtle, _: Turtle, Some(weightVariable))
    meanPathLength(gc.turtles, dist)
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}