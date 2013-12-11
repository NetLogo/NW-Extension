// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.turtleCreatingCommand
import org.nlogo.extensions.nw.jung.Generator
import org.nlogo.agent

class BarabasiAlbertGenerator extends turtleCreatingCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    new Generator(
      turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      linkBreed = args(1).getAgentSet.requireLinkBreed)
      .barabasiAlbert(getIntValueWithMinimum(args(2), 1), context.getRNG)
}

class KleinbergSmallWorldGenerator extends turtleCreatingCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, NumberType, NumberType, BooleanType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    new Generator(
      turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      linkBreed = args(1).getAgentSet.requireLinkBreed)
      .kleinbergSmallWorld(
        rowCount = getIntValueWithMinimum(args(2), 2, "rows"),
        colCount = getIntValueWithMinimum(args(3), 2, "columns"),
        clusteringExponent = args(4).getDoubleValue,
        isToroidal = args(5).getBooleanValue,
        rng = context.getRNG)
}

class Lattice2DGenerator extends turtleCreatingCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, NumberType, BooleanType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    new Generator(
      turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      linkBreed = args(1).getAgentSet.requireLinkBreed)
      .lattice2D(
        rowCount = getIntValueWithMinimum(args(2), 2, "rows"),
        colCount = getIntValueWithMinimum(args(3), 2, "columns"),
        isToroidal = args(4).getBooleanValue,
        rng = context.getRNG)
}