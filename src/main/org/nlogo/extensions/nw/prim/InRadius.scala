// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.agent
import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.algorithms.InRadius._
import scala.collection.JavaConverters._
import org.nlogo.extensions.nw.util.TurtleSetsConverters._
import org.nlogo.agent.AgentSet

trait InRadiusPrim extends api.DefaultReporter {
  val getGraphContext: api.World => GraphContext
  val followUnLinks: Boolean = true
  val followInLinks: Boolean
  val followOutLinks: Boolean
  override def getSyntax = reporterSyntax(
    Array(NumberType),
    TurtlesetType,
    "-T--")
  override def report(args: Array[api.Argument], context: api.Context) = {
    val world = context.getAgent.world.asInstanceOf[agent.World]
    val graphContext = getGraphContext(context.getAgent.world)
    val source = context.getAgent.asInstanceOf[agent.Turtle]
    val radius = args(0).getIntValue
    if (radius < 0) throw new api.ExtensionException("radius cannot be negative")
    inRadius(graphContext, source, radius, followUnLinks, followInLinks, followOutLinks)
  }
}

class TurtlesInRadius(
  override val getGraphContext: api.World => GraphContext)
  extends InRadiusPrim {
  override val followInLinks = false
  override val followOutLinks = true
}

class TurtlesInReverseRadius(
  override val getGraphContext: api.World => GraphContext)
  extends InRadiusPrim {
  override val followInLinks = true
  override val followOutLinks = false
}
