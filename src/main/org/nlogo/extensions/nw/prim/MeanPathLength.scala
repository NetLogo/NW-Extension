package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.algorithms.MeanPathLength._
import org.nlogo.agent.Turtle

class MeanPathLength(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(NumberType | BooleanType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val gc = getGraphContext(context.getAgent.world)
    val dist = gc.distance(_: Turtle, _: Turtle).map(_.toDouble)
    meanPathLength(gc.turtles, dist)
      .map(Double.box)
      .getOrElse(java.lang.Boolean.FALSE)
  }
}
