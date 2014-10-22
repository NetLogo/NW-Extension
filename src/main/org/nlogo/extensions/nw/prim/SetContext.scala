// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.{Context, Argument, ExtensionException, LogoException}
import org.nlogo.api.Syntax._
import org.nlogo.agent
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.nvm.{AssemblerAssistant, CustomAssembled, ExtensionContext}
import org.nlogo.extensions.nw.util.TurtleSetsConverters
import org.nlogo.agent.ArrayAgentSet

class SetContext(setContext: GraphContext => Unit)
  extends api.DefaultCommand {
  override def getSyntax = commandSyntax(
    Array(AgentsetType, AgentsetType))
  override def perform(args: Array[api.Argument], context: api.Context) {
    val turtleSet = args(0).getAgentSet.requireTurtleSet
    val linkSet = args(1).getAgentSet.requireLinkSet
    val world = context.getAgent.world.asInstanceOf[agent.World]
    val gc = new GraphContext(world, turtleSet, linkSet)
    setContext(gc)
  }
}

class GetContext(getGraphContext: api.World => GraphContext)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(ListType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val gc = getGraphContext(context.getAgent.world.asInstanceOf[org.nlogo.agent.World])
    val workspace = context.asInstanceOf[ExtensionContext].workspace
    api.LogoList(gc.turtleSet, gc.linkSet)
  }
}

class WithContext(setGraphContext: GraphContext => Unit, getGraphContext: api.World => GraphContext)
  extends api.DefaultCommand
  with CustomAssembled{
  override def getSyntax = commandSyntax(
    Array(AgentsetType, AgentsetType, CommandBlockType))

  def perform(args: Array[Argument], context: Context) {
    val turtleSet = args(0).getAgentSet.requireTurtleSet
    val linkSet = args(1).getAgentSet.requireLinkSet
    val world = context.getAgent.world.asInstanceOf[agent.World]
    val gc = new GraphContext(world, turtleSet, linkSet)
    val extContext = context.asInstanceOf[ExtensionContext]
    val nvmContext = extContext.nvmContext
    // Note that this can optimized by hanging onto the array and just mutating it. Shouldn't be necessary though.
    val agentSet = new ArrayAgentSet(nvmContext.agent.kind, null, Array(nvmContext.agent))
    val currentContext = getGraphContext(world)
    setGraphContext(gc)
    nvmContext.runExclusiveJob(agentSet, nvmContext.ip + 1)
    setGraphContext(currentContext)
  }

  def assemble(a: AssemblerAssistant) {
    a.block()
    a.done()
  }
}

