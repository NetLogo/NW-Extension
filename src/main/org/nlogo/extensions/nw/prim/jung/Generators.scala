// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleCreatingCommand
import org.nlogo.extensions.nw.jung.Generator
import org.nlogo.agent

class BarabasiAlbertGenerator extends TurtleCreatingCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) = {
    val world = context.getAgent.world.asInstanceOf[agent.World]
    new Generator(
      turtleBreed = args(0).getAgentSet.requireTurtleBreed(world),
      linkBreed = args(1).getAgentSet.requireLinkBreed(world),
      world = world)
      .barabasiAlbert(getIntValueWithMinimum(args(2), 1), context.getRNG)
    }
}

class KleinbergSmallWorldGenerator extends TurtleCreatingCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, NumberType, NumberType, BooleanType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) = {
    val world = context.getAgent.world.asInstanceOf[agent.World]
    new Generator(
      turtleBreed = args(0).getAgentSet.requireTurtleBreed(world),
      linkBreed = args(1).getAgentSet.requireLinkBreed(world),
      world = world)
      .kleinbergSmallWorld(
        rowCount = getIntValueWithMinimum(args(2), 2, "rows"),
        colCount = getIntValueWithMinimum(args(3), 2, "columns"),
        clusteringExponent = args(4).getDoubleValue,
        isToroidal = args(5).getBooleanValue,
        rng = context.getRNG)
    }
}

class Lattice2DGenerator extends TurtleCreatingCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, NumberType, BooleanType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) = {
    val world = context.getAgent.world.asInstanceOf[agent.World]
    new Generator(
      turtleBreed = args(0).getAgentSet.requireTurtleBreed(world),
      linkBreed = args(1).getAgentSet.requireLinkBreed(world),
      world = world)
      .lattice2D(
        rowCount = getIntValueWithMinimum(args(2), 2, "rows"),
        colCount = getIntValueWithMinimum(args(3), 2, "columns"),
        isToroidal = args(4).getBooleanValue,
        rng = context.getRNG)
    }
}
