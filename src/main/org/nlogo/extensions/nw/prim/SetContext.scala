// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.ExtensionException
import org.nlogo.api.LogoException
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.nvm.ExtensionContext

class SetContext(setContext: GraphContext => Unit)
  extends api.DefaultCommand {
  override def getSyntax = commandSyntax(
    Array(AgentsetType, AgentsetType))
  override def perform(args: Array[api.Argument], context: api.Context) {
    val turtleSet = args(0).getAgentSet.requireTurtleSet
    val linkSet = args(1).getAgentSet.requireLinkSet
    val world = linkSet.world
    val gc = new GraphContext(world, turtleSet, linkSet)
    setContext(gc)
  }
}

class GetContext(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val gc = getGraphContext(context.getAgent.world.asInstanceOf[org.nlogo.agent.World])
    val workspace = context.asInstanceOf[ExtensionContext].workspace()
    api.LogoList(gc.turtleSet, gc.linkSet)
  }
}
