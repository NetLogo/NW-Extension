// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.GraphContext

class SetContext(setContext: GraphContext => Unit)
  extends api.DefaultCommand {
  override def getSyntax = commandSyntax(
    Array(AgentsetType, AgentsetType))
  override def perform(args: Array[api.Argument], context: api.Context) {
    val turtleSet = args(0).getAgentSet.requireTurtleBreed
    val linkSet = args(1).getAgentSet.requireLinkBreed
    setContext(new GraphContext(turtleSet, linkSet))
  }
}
