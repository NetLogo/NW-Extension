// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetLogoGraph
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToNetLogoAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.StaticNetLogoGraph

class SetSnapshot(setGraph: NetLogoGraph => Unit)
  extends api.DefaultCommand {
  override def getSyntax = commandSyntax(
    Array(AgentsetType, AgentsetType))
  override def perform(args: Array[api.Argument], context: api.Context) {
    val turtleSet = args(0).getAgentSet.requireTurtleSet
    val linkSet = args(1).getAgentSet.requireLinkSet
    setGraph(new StaticNetLogoGraph(linkSet, turtleSet))
  }
}