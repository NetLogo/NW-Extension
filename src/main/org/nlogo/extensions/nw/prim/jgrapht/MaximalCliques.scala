// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jgrapht

import org.nlogo.api
import org.nlogo.api.ScalaConversions.toLogoList
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetLogoGraph

trait CliquePrim
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  val getGraph: api.Context => NetLogoGraph
  def graph(context: api.Context) = {
    val g = getGraph(context)
    if (!g.isUndirected)
      throw new api.ExtensionException("Current graph must be undirected")
    g.asJGraphTGraph
  }
}

class MaximalCliques(
  override val getGraph: api.Context => NetLogoGraph)
  extends CliquePrim {
  override def report(args: Array[api.Argument], context: api.Context) = {
    toLogoList(
      graph(context)
        .BronKerboschCliqueFinder
        .allCliques(context.getRNG))
  }
}

class BiggestMaximalCliques(
  override val getGraph: api.Context => NetLogoGraph)
  extends CliquePrim {
  override def report(args: Array[api.Argument], context: api.Context) = {
    toLogoList(
      graph(context)
        .BronKerboschCliqueFinder
        .biggestCliques(context.getRNG))
  }
}
