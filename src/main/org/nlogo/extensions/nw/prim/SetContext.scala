// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.agent.ArrayAgentSet
import org.nlogo.api
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.GraphContextManager
import org.nlogo.extensions.nw.GraphContextProvider
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.nvm.AssemblerAssistant
import org.nlogo.nvm.CustomAssembled
import org.nlogo.nvm.ExtensionContext

class SetContext(gcm: GraphContextManager)
  extends api.DefaultCommand {
  override def getSyntax = commandSyntax(
    Array(AgentsetType, AgentsetType))
  override def perform(args: Array[api.Argument], context: api.Context) {
    val turtleSet = args(0).getAgentSet.requireTurtleSet
    val linkSet = args(1).getAgentSet.requireLinkSet
    val world = linkSet.world
    val gc = new GraphContext(world, turtleSet, linkSet)
    gcm.setGraphContext(gc)
  }
}

class GetContext(gcp: GraphContextProvider)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val gc = gcp.getGraphContext(context.getAgent.world.asInstanceOf[org.nlogo.agent.World])
    val workspace = context.asInstanceOf[ExtensionContext].workspace()
    api.LogoList(gc.turtleSet, gc.linkSet)
  }
}

class WithContext(gcp: GraphContextProvider)
  extends api.DefaultCommand
  with CustomAssembled {
  override def getSyntax = commandSyntax(
    Array(AgentsetType, AgentsetType, CommandBlockType))

  def perform(args: Array[Argument], context: Context) {
    val turtleSet = args(0).getAgentSet.requireTurtleSet
    val linkSet = args(1).getAgentSet.requireLinkSet
    val world = linkSet.world
    val gc = new GraphContext(world, turtleSet, linkSet)
    val extContext = context.asInstanceOf[ExtensionContext]
    val nvmContext = extContext.nvmContext
    // Note that this can optimized by hanging onto the array and just mutating it. Shouldn't be necessary though.
    val agentSet = new ArrayAgentSet(nvmContext.agent.getAgentClass, Array(nvmContext.agent), world)
    gcp.withTempGraphContext(gc) { () =>
      nvmContext.runExclusiveJob(agentSet, nvmContext.ip + 1)
    }
  }

  def assemble(a: AssemblerAssistant) {
    a.block()
    a.done()
  }
}
