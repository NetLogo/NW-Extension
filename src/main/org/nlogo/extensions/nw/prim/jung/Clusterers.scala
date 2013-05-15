// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.ScalaConversions.toLogoList
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetLogoGraph

class BicomponentClusters(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  override def report(args: Array[api.Argument], context: api.Context) =
    toLogoList(getGraph(context).asUndirectedJungGraph
      .BicomponentClusterer
      .clusters(context.getRNG))
}

class WeakComponentClusters(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  override def report(args: Array[api.Argument], context: api.Context) =
    toLogoList(getGraph(context).asJungGraph
      .WeakComponentClusterer
      .clusters(context.getRNG))
}