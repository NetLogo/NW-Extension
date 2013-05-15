// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.ScalaConversions.toLogoList
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext

class BicomponentClusters(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = getGraphContext(context.getAgent.world).asUndirectedJungGraph
    toLogoList(graph
      .BicomponentClusterer
      .clusters(context.getRNG))
  }
}

class WeakComponentClusters(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = getGraphContext(context.getAgent.world).asJungGraph
    toLogoList(graph
      .WeakComponentClusterer
      .clusters(context.getRNG))
  }
}
