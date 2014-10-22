// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.agent
import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleCreatingCommand
import org.nlogo.extensions.nw.jung.io.Matrix
import org.nlogo.extensions.nw.GraphContext

class SaveMatrix(getGraphContext: api.World => GraphContext)
  extends api.DefaultCommand {
  override def getSyntax = commandSyntax(Array(StringType))
  override def perform(args: Array[api.Argument], context: api.Context) {
    val graph = getGraphContext(context.getAgent.world).asJungGraph
    Matrix.save(graph, args(0).getString)
  }
}

class LoadMatrix
  extends TurtleCreatingCommand {
  override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) = {
    val world = context.getAgent.world.asInstanceOf[agent.World]
    Matrix.load(
      filename = args(0).getString,
      turtleBreed = args(1).getAgentSet.requireTurtleBreed(world),
      linkBreed = args(2).getAgentSet.requireLinkBreed(world),
      rng = context.getRNG,
      world)
  }
}
