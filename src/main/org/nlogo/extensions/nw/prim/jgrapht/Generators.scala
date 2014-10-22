// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jgrapht

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleCreatingCommand
import org.nlogo.extensions.nw.jgrapht.Generator
import org.nlogo.agent.World

trait SimpleGeneratorPrim
  extends TurtleCreatingCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
  def turtleBreed(args: Array[api.Argument], world: World) =
    args(0).getAgentSet.requireTurtleBreed(world)
  def linkBreed(args: Array[api.Argument], world: World) =
    args(1).getAgentSet.requireLinkBreed(world)
  def generator(args: Array[api.Argument], context: api.Context) =
    new Generator(turtleBreed(args, context.getAgent.world.asInstanceOf[World]),
      linkBreed(args, context.getAgent.world.asInstanceOf[World]),
      context.getAgent.world.asInstanceOf[World])
}

trait SimpleUndirectedGeneratorPrim extends SimpleGeneratorPrim {
  override def linkBreed(args: Array[api.Argument], world: World) =
    args(1).getAgentSet.requireUndirectedLinkBreed(world)
}

trait SimpleDirectedGeneratorPrim extends SimpleGeneratorPrim {
  override def linkBreed(args: Array[api.Argument], world: World) =
    args(1).getAgentSet.requireDirectedLinkBreed(world)
}

class RingGenerator extends SimpleGeneratorPrim {
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    generator(args, context)
      .ringGraphGenerator(getIntValueWithMinimum(args(2), 3), context.getRNG)
}

class StarGenerator extends SimpleGeneratorPrim {
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    generator(args, context)
      .starGraphGenerator(getIntValueWithMinimum(args(2), 1), context.getRNG)
}

class WheelGenerator extends SimpleUndirectedGeneratorPrim {
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    generator(args, context)
      .wheelGraphGenerator(getIntValueWithMinimum(args(2), 4), true, context.getRNG)
}

class WheelGeneratorInward extends SimpleDirectedGeneratorPrim {
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    generator(args, context)
      .wheelGraphGenerator(getIntValueWithMinimum(args(2), 4), true, context.getRNG)
}

class WheelGeneratorOutward extends SimpleDirectedGeneratorPrim {
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    generator(args, context)
      .wheelGraphGenerator(getIntValueWithMinimum(args(2), 4), false, context.getRNG)
}
