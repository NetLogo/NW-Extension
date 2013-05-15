// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jgrapht

import org.nlogo.api
import org.nlogo.api.ScalaConversions.toLogoList
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext

trait CliquePrim
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  val getGraphContext: api.World => GraphContext
  def graph(context: api.Context) = {
    val gc = getGraphContext(context.getAgent.world)
    if (gc.isDirected)
      throw new api.ExtensionException("Current graph must be undirected")
    gc.asJGraphTGraph
  }
}

class MaximalCliques(
  override val getGraphContext: api.World => GraphContext)
  extends CliquePrim {
  override def report(args: Array[api.Argument], context: api.Context) = {
    toLogoList(
      graph(context)
        .BronKerboschCliqueFinder
        .allCliques(context.getRNG))
  }
}

class BiggestMaximalCliques(
  override val getGraphContext: api.World => GraphContext)
  extends CliquePrim {
  override def report(args: Array[api.Argument], context: api.Context) = {
    toLogoList(
      graph(context)
        .BronKerboschCliqueFinder
        .biggestCliques(context.getRNG))
  }
}
