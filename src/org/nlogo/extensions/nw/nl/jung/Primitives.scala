package org.nlogo.extensions.nw.nl.jung

import scala.Array.canBuildFrom

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

trait Primitives {
  self: NetworkExtension =>

  object Snapshot extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(AgentsetType, AgentsetType))
    override def perform(args: Array[Argument], context: Context) {
      val turtleSet = args(0).getAgentSet.requireTurtleSet
      val linkSet = args(1).getAgentSet.requireLinkSet
      setGraph(new StaticNetLogoGraph(linkSet, turtleSet))
    }
  }

  object KMeansClusters extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(NumberType, NumberType, NumberType),
      ListType)
    override def report(args: Array[Argument], context: Context) = {
      getGraph(context).asJungGraph
        .kMeansClusterer
        .clusters(
          nbClusters = args(0).getIntValue,
          maxIterations = args(1).getIntValue,
          convergenceThreshold = args(2).getDoubleValue)
        .toLogoList
    }
  }

  object BicomponentClusters extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context) = {
      getGraph(context).asUndirectedJungGraph
        .bicomponentClusterer
        .clusters
        .toLogoList
    }
  }

  object WeakComponentClusters extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context) = {
      getGraph(context).asUndirectedJungGraph
        .weakComponentClusterer
        .clusters
        .toLogoList
    }
  }

  object BetweennessCentralityPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType, agentClassString = "-T-L")
    override def report(args: Array[Argument], context: Context) =
      getGraph(context).asJungGraph
        .betweennessCentrality
        .get(context.getAgent)
        .toLogoObject
  }

  object EigenvectorCentralityPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType, agentClassString = "-T--")
    override def report(args: Array[Argument], context: Context) =
      getGraph(context).asJungGraph
        .eigenvectorCentrality
        .getVertexScore(context.getAgent.asInstanceOf[Turtle])
        .toLogoObject
  }

  object ClosenessCentralityPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType, agentClassString = "-T--")
    override def report(args: Array[Argument], context: Context) =
      getGraph(context).asJungGraph
        .closenessCentrality
        .getVertexScore(context.getAgent.asInstanceOf[Turtle])
        .toLogoObject
  }

  object LinkPath extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType),
      ListType,
      agentClassString = "-T--")
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[Turtle]
      val target = args(0).getAgent.asInstanceOf[Turtle]
      LogoList.fromJava(
        getGraph(context).asJungGraph
          .dijkstraShortestPath
          .getPath(source, target))
    }
  }

  object MeanLinkPathLength extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[Turtle]
      val target = args(0).getAgent.asInstanceOf[Turtle]
      getGraph(context).asJungGraph
        .dijkstraShortestPath
        .meanLinkPathLength
        .map(Double.box)
        .getOrElse(java.lang.Boolean.FALSE)
    }
  }

  object LinkDistance extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType),
      NumberType | BooleanType,
      agentClassString = "-T--")
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[Turtle]
      val target = args(0).getAgent.asInstanceOf[Turtle]
      val path = getGraph(context).asJungGraph
        .dijkstraShortestPath
        .getPath(source, target)
      Option(path.size).filterNot(0==).getOrElse(false).toLogoObject
    }
  }

  object EppsteinPowerLawGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, NumberType, NumberType))
    override def perform(args: Array[Argument], context: Context) {
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .eppsteinPowerLaw(
          nbVertices = args(2).getIntValue,
          nbEdges = args(3).getIntValue,
          nbIterations = args(4).getIntValue)
    }
  }

  object BarabasiAlbertGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, NumberType, NumberType))
    override def perform(args: Array[Argument], context: Context) {
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .barabasiAlbert(
          initialNbVertices = args(2).getIntValue,
          nbEdgesPerIteration = args(3).getIntValue,
          nbIterations = args(4).getIntValue)
    }
  }

  object ErdosRenyiGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, NumberType, CommandTaskType | OptionalType))
    override def perform(args: Array[Argument], context: Context) {

      println(args.map(_.get).mkString("\n"))
      val (newTurtles, temp) = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .erdosRenyi(
          nbVertices = args(2).getIntValue,
          connexionProbability = args(3).getDoubleValue)
        .duplicate
      println(temp.toList)
      println(args(4))
      //      val command = args(4).getCommandTask.asInstanceOf[nvm.CommandTask]
      val commandArgs = Array[AnyRef]()
      val nvmContext = context.asInstanceOf[ExtensionContext].nvmContext

    }
  }

  object KleinbergSmallWorldGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, NumberType, NumberType, BooleanType))
    override def perform(args: Array[Argument], context: Context) {
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .kleinbergSmallWorld(
          rowCount = args(2).getIntValue,
          colCount = args(3).getIntValue,
          clusteringExponent = args(4).getDoubleValue,
          isToroidal = args(5).getBooleanValue)
    }
  }

  object Lattice2DGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, NumberType, BooleanType))
    override def perform(args: Array[Argument], context: Context) {
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .lattice2D(
          rowCount = args(2).getIntValue,
          colCount = args(3).getIntValue,
          isToroidal = args(4).getBooleanValue)
    }
  }

  object SaveMatrix extends DefaultCommand {
    override def getSyntax = commandSyntax(Array(StringType))
    override def perform(args: Array[Argument], context: Context) {
      Matrix.save(getGraph(context).asJungGraph, args(0).getString)
    }
  }

  object LoadMatrix extends DefaultCommand {
    override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType))
    override def perform(args: Array[Argument], context: Context) {
      Matrix.load(
        filename = args(0).getString,
        turtleBreed = args(1).getAgentSet.requireTurtleBreed,
        linkBreed = args(2).getAgentSet.requireLinkBreed)
    }
  }
}