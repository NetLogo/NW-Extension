package org.nlogo.extensions.nw.nl.jgrapht

import org.nlogo.extensions.nw.NetworkExtension
import org.nlogo.extensions.nw.nl.jgrapht
import org.nlogo.api.ScalaConversions.toRichAny
import org.nlogo.api.ScalaConversions.toRichSeq
import org.nlogo.api.Syntax.AgentsetType
import org.nlogo.api.Syntax.BooleanType
import org.nlogo.api.Syntax.CommandTaskType
import org.nlogo.api.Syntax.LinksetType
import org.nlogo.api.Syntax.ListType
import org.nlogo.api.Syntax.NumberType
import org.nlogo.api.Syntax.OptionalType
import org.nlogo.api.Syntax.StringType
import org.nlogo.api.Syntax.TurtleType
import org.nlogo.api.Syntax.TurtlesetType
import org.nlogo.api.Syntax.commandSyntax
import org.nlogo.api.Syntax.reporterSyntax
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.DefaultCommand
import org.nlogo.api.DefaultReporter
import org.nlogo.api.LogoList
import org.nlogo.api.Turtle
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToNetLogoAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentToNetLogoAgent
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleToNetLogoTurtle
import org.nlogo.extensions.nw.NetworkExtension
import org.nlogo.extensions.nw.StaticNetLogoGraph
import org.nlogo.nvm.ExtensionContext
import scala.collection.JavaConverters._

trait Primitives {
  self: NetworkExtension =>

  object MaximalCliques extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context) = {
      getGraph(context).asJGraphTGraph
        .bronKerboschCliqueFinder
        .cliques
        .toLogoList
    }
  }

  object BiggestMaximalClique extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context) = {
      getGraph(context).asJGraphTGraph
        .bronKerboschCliqueFinder
        .biggestClique
        .toLogoList
    }
  }

  object RingGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType))
    override def perform(args: Array[Argument], context: Context) {
      // TODO: uncomment when jgrapht.Generator is pushed to github...
      //      new jgrapht.Generator(
      //        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      //        linkBreed = args(1).getAgentSet.requireLinkBreed)
      //        .ringGraphGenerator(args(2).getIntValue)
    }
  }

  object StarGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType))
    override def perform(args: Array[Argument], context: Context) {
      // TODO: uncomment when jgrapht.Generator is pushed to github...
      //      new jgrapht.Generator(
      //        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      //        linkBreed = args(1).getAgentSet.requireLinkBreed)
      //        .starGraphGenerator(args(2).getIntValue)
    }
  }

  object WheelGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType))
    override def perform(args: Array[Argument], context: Context) {
      // TODO: uncomment when jgrapht.Generator is pushed to github...
      //      new jgrapht.Generator(
      //        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      //        linkBreed = args(1).getAgentSet.requireUndirectedLinkBreed)
      //        .wheelGraphGenerator(args(2).getIntValue, true)
    }
  }

  object WheelGeneratorInwardPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType))
    override def perform(args: Array[Argument], context: Context) {
      // TODO: uncomment when jgrapht.Generator is pushed to github...
      //      new jgrapht.Generator(
      //        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      //        linkBreed = args(1).getAgentSet.requireDirectedLinkBreed)
      //        .wheelGraphGenerator(args(2).getIntValue, true)
    }
  }

  object WheelGeneratorOutwardPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType))
    override def perform(args: Array[Argument], context: Context) {
      // TODO: uncomment when jgrapht.Generator is pushed to github...
      //      new jgrapht.Generator(
      //        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      //        linkBreed = args(1).getAgentSet.requireDirectedLinkBreed)
      //        .wheelGraphGenerator(args(2).getIntValue, false)
    }
  }

}