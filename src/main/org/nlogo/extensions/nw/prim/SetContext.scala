// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.nvm.ExtensionContext
import org.nlogo.nvm.Workspace

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

class ShowContext(getGraphContext: api.World => GraphContext)
  extends api.DefaultCommand {
  override def getSyntax = commandSyntax()
  override def perform(args: Array[api.Argument], context: api.Context) {
    val gc = getGraphContext(context.getAgent.world.asInstanceOf[org.nlogo.agent.World])
    val workspace = context.asInstanceOf[ExtensionContext].workspace()
    workspace.outputObject(
      gc.toString, null, true, false,
      Workspace.OutputDestination.NORMAL)
  }
}
