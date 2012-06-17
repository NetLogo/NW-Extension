package org.nlogo.extensions.nw.jgrapht

import org.nlogo.extensions.nw.NetworkExtensionUtil.runCommandTaskForTurtles
import org.nlogo.api.ScalaConversions.toLogoList
import org.nlogo.api.Syntax.LinksetType
import org.nlogo.api.Syntax.ListType
import org.nlogo.api.Syntax.NumberType
import org.nlogo.api.Syntax.TurtlesetType
import org.nlogo.api.Syntax.CommandTaskType
import org.nlogo.api.Syntax.commandSyntax
import org.nlogo.api.Syntax.reporterSyntax
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.DefaultCommand
import org.nlogo.api.DefaultReporter
import org.nlogo.api.ExtensionException
import org.nlogo.nvm
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToNetLogoAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtension

trait Primitives {
  self: NetworkExtension =>

  object MaximalCliques extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context) = {
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
    override def report(args: Array[Argument], context: Context) = {
      val g = getGraph(context)
      // TODO: This should probably be dealt with in graph construction:
      if (!g.isUndirected) throw new ExtensionException("Current graph must be undirected")
      toLogoList(g.asJGraphTGraph
        .bronKerboschCliqueFinder
        .biggestClique(context.getRNG))
    }
  }

  object RingGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandTaskType))
    override def perform(args: Array[Argument], context: Context) {
      val newTurtles = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .ringGraphGenerator(args(2).getIntValue, context.getRNG)
      runCommandTaskForTurtles(newTurtles, args(3), context)
    }
  }

  object StarGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandTaskType))
    override def perform(args: Array[Argument], context: Context) {
      val newTurtles = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .starGraphGenerator(args(2).getIntValue, context.getRNG)
      runCommandTaskForTurtles(newTurtles, args(3), context)
    }
  }

  object WheelGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandTaskType))
    override def perform(args: Array[Argument], context: Context) {
      val newTurtles = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireUndirectedLinkBreed)
        .wheelGraphGenerator(args(2).getIntValue, true, context.getRNG)
      runCommandTaskForTurtles(newTurtles, args(3), context)
    }
  }

  object WheelGeneratorInwardPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandTaskType))
    override def perform(args: Array[Argument], context: Context) {
      val newTurtles = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireDirectedLinkBreed)
        .wheelGraphGenerator(args(2).getIntValue, true, context.getRNG)
      runCommandTaskForTurtles(newTurtles, args(3), context)
    }
  }

  object WheelGeneratorOutwardPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandTaskType))
    override def perform(args: Array[Argument], context: Context) {
      val newTurtles = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireDirectedLinkBreed)
        .wheelGraphGenerator(args(2).getIntValue, false, context.getRNG)
      runCommandTaskForTurtles(newTurtles, args(3), context)
    }
  }

}