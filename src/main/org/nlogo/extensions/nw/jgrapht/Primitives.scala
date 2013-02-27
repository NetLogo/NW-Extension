// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jgrapht

import org.nlogo.extensions.nw.NetworkExtensionUtil.turtleCreatingCommand
import org.nlogo.api.ScalaConversions._
import org.nlogo.api
import api.Syntax._
import api.DefaultReporter
import api.DefaultCommand
import api.ExtensionException
import org.nlogo.nvm
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToNetLogoAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtension

trait Primitives {
  self: NetworkExtension =>

  object MaximalCliques extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[api.Argument], context: api.Context) = {
      val g = getGraph(context)
      // TODO: This should probably be dealt with in graph construction:
      if (!g.isUndirected) throw new ExtensionException("Current graph must be undirected")
      toLogoList(g.asJGraphTGraph
        .BronKerboschCliqueFinder
        .allCliques(context.getRNG))
    }
  }

  object BiggestMaximalCliques extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[api.Argument], context: api.Context) = {
      val g = getGraph(context)
      // TODO: This should probably be dealt with in graph construction:
      if (!g.isUndirected) throw new ExtensionException("Current graph must be undirected")
      toLogoList(g.asJGraphTGraph
        .BronKerboschCliqueFinder
        .biggestCliques(context.getRNG))
    }
  }

  object RingGeneratorPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .ringGraphGenerator(getIntValueWithMinimum(args(2), 3), context.getRNG)
  }

  object StarGeneratorPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .starGraphGenerator(getIntValueWithMinimum(args(2), 1), context.getRNG)
  }

  object WheelGeneratorPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireUndirectedLinkBreed)
        .wheelGraphGenerator(getIntValueWithMinimum(args(2), 4), true, context.getRNG)
  }

  object WheelGeneratorInwardPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireDirectedLinkBreed)
        .wheelGraphGenerator(getIntValueWithMinimum(args(2), 4), true, context.getRNG)
  }

  object WheelGeneratorOutwardPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireDirectedLinkBreed)
        .wheelGraphGenerator(getIntValueWithMinimum(args(2), 4), false, context.getRNG)
  }

}