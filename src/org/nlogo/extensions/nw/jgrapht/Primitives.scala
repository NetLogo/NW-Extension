package org.nlogo.extensions.nw.jgrapht

import org.nlogo.extensions.nw.NetworkExtensionUtil.turtleCreatingCommand
import org.nlogo.api.ScalaConversions.toLogoList
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
        .bronKerboschCliqueFinder
        .cliques)
    }
  }

  object BiggestMaximalClique extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[api.Argument], context: api.Context) = {
      val g = getGraph(context)
      // TODO: This should probably be dealt with in graph construction:
      if (!g.isUndirected) throw new ExtensionException("Current graph must be undirected")
      toLogoList(g.asJGraphTGraph
        .bronKerboschCliqueFinder
        .biggestClique(context.getRNG))
    }
  }

  object RingGeneratorPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) = {
      val nbTurtles = args(2).getIntValue
      if (nbTurtles < 3) throw new ExtensionException("The number of turtles in a ring network must be at least 3.")
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .ringGraphGenerator(nbTurtles, context.getRNG)
    }
  }

  object StarGeneratorPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .starGraphGenerator(args(2).getIntValue, context.getRNG)
  }

  object WheelGeneratorPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) = {
      val nbTurtles = args(2).getIntValue
      if (nbTurtles < 4) 
        throw new ExtensionException("The number of turtles in a wheel network must be at least 4.")
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireUndirectedLinkBreed)
        .wheelGraphGenerator(nbTurtles, true, context.getRNG)
    }
  }

  object WheelGeneratorInwardPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireDirectedLinkBreed)
        .wheelGraphGenerator(args(2).getIntValue, true, context.getRNG)
  }

  object WheelGeneratorOutwardPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireDirectedLinkBreed)
        .wheelGraphGenerator(args(2).getIntValue, false, context.getRNG)
  }

}