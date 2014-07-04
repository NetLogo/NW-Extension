// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.ScalaConversions.toLogoList
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.GraphContextProvider

class BicomponentClusters(gcp: GraphContextProvider)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world).asUndirectedJungGraph
    toLogoList(graph
      .BicomponentClusterer
      .clusters(context.getRNG))
  }
}

class WeakComponentClusters(gcp: GraphContextProvider)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  override def report(args: Array[api.Argument], context: api.Context) = {
    val graph = gcp.getGraphContext(context.getAgent.world).asJungGraph
    toLogoList(graph
      .WeakComponentClusterer
      .clusters(context.getRNG))
  }
}
