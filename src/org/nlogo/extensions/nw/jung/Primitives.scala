package org.nlogo.extensions.nw.jung

import scala.collection.JavaConverters.asScalaBufferConverter
import org.nlogo.api.ScalaConversions.toLogoList
import org.nlogo.api.ScalaConversions.toLogoObject
import org.nlogo.api.Syntax.AgentsetType
import org.nlogo.api.Syntax.BooleanType
import org.nlogo.api.Syntax.CommandTaskType
import org.nlogo.api.Syntax.LinksetType
import org.nlogo.api.Syntax.ListType
import org.nlogo.api.Syntax.NumberType
import org.nlogo.api.Syntax.StringType
import org.nlogo.api.Syntax.TurtleType
import org.nlogo.api.Syntax.TurtlesetType
import org.nlogo.api.Syntax.commandSyntax
import org.nlogo.api.Syntax.reporterSyntax
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.DefaultCommand
import org.nlogo.api.DefaultReporter
import org.nlogo.api.ExtensionException
import org.nlogo.api.LogoList
import org.nlogo.api.Turtle
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToNetLogoAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentToNetLogoAgent
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleToNetLogoTurtle
import org.nlogo.extensions.nw.NetworkExtensionUtil.runCommandTaskForTurtles
import org.nlogo.extensions.nw.NetworkExtension
import org.nlogo.extensions.nw.StaticNetLogoGraph

import edu.uci.ics.jung.algorithms.filters.KNeighborhoodFilter
trait Primitives {
  self: NetworkExtension =>

  object KMeansClusters extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(NumberType, NumberType, NumberType),
      ListType)
    override def report(args: Array[Argument], context: Context) =
      toLogoList(getGraph(context).asJungGraph
        .kMeansClusterer
        .clusters(
          nbClusters = args(0).getIntValue,
          maxIterations = args(1).getIntValue,
          convergenceThreshold = args(2).getDoubleValue,
          rng = context.getRNG))
  }

  object BicomponentClusters extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context) =
      toLogoList(getGraph(context).asUndirectedJungGraph
        .bicomponentClusterer
        .clusters)
  }

  object WeakComponentClusters extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context) =
      toLogoList(getGraph(context).asJungGraph
        .weakComponentClusterer
        .clusters)
  }

  object BetweennessCentralityPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType, "-T-L")
    override def report(args: Array[Argument], context: Context) =
      Double.box(getGraph(context).asJungGraph
        .betweennessCentrality
        .get(context.getAgent))
  }

  object EigenvectorCentralityPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType, "-T--")
    override def report(args: Array[Argument], context: Context) = {
      val g = getGraph(context).asUndirectedJungGraph
      // make sure graph is connected
      if (g.isWeaklyConnected) // TODO: Actually, it should be STRONGLY connected
        g.eigenvectorCentrality
          .getScore(context.getAgent.asInstanceOf[Turtle])
      else
        java.lang.Boolean.FALSE
    }
  }

  object ClosenessCentralityPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType, "-T--")
    override def report(args: Array[Argument], context: Context) =
      getGraph(context).asJungGraph
        .closenessCentrality
        .getScore(context.getAgent.asInstanceOf[Turtle])
  }

  object PathToPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType),
      ListType,
      "-T--")
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[Turtle]
      val target = args(0).getAgent.asInstanceOf[Turtle]
      LogoList.fromJava(
        getGraph(context).asJungGraph
          .dijkstraShortestPath
          .getPath(source, target))
    }
  }

  def linkPathToTurtlePath(
    source: org.nlogo.api.Turtle,
    linkPath: java.util.List[org.nlogo.agent.Link]) =
    if (linkPath.isEmpty) Vector()
    else linkPath.asScala.foldLeft(Vector(source)) {
      case (turtles, link) =>
        turtles :+ (if (link.end1 != turtles.last) link.end1 else link.end2)
    }

  object TurtlesOnPathToPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType),
      ListType,
      "-T--")
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[Turtle]
      val target = args(0).getAgent.asInstanceOf[Turtle]
      val path =
        if (source == target)
          Vector(source)
        else {
          val graph = getGraph(context).asJungGraph
          val linkPath = graph.dijkstraShortestPath.getPath(source, target)
          linkPathToTurtlePath(source, linkPath)
        }
      LogoList.fromVector(path)
    }
  }

  object TurtlesOnWeightedPathToPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType, StringType),
      ListType,
      "-T--")
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[Turtle]
      val target = args(0).getAgent.asInstanceOf[Turtle]

      val path =
        if (source == target)
          Vector(source)
        else {
          val graph = getGraph(context).asJungGraph
          val weightVariable = args(1).getString.toUpperCase
          val linkPath = graph.dijkstraShortestPath(weightVariable).getPath(source, target)
          linkPathToTurtlePath(source, linkPath)
        }
      LogoList.fromVector(path)
    }
  }

  object MeanPathLengthPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType | BooleanType)
    override def report(args: Array[Argument], context: Context): AnyRef = {
      getGraph(context).asJungGraph
        .dijkstraShortestPath
        .meanLinkPathLength
        .map(Double.box)
        .getOrElse(java.lang.Boolean.FALSE)
    }
  }

  object MeanWeightedPathLengthPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(StringType),
      NumberType | BooleanType)
    override def report(args: Array[Argument], context: Context): AnyRef = {
      getGraph(context).asJungGraph
        .dijkstraShortestPath(args(0).getString.toUpperCase)
        .meanLinkPathLength
        .map(Double.box)
        .getOrElse(java.lang.Boolean.FALSE)
    }
  }

  object DistanceToPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType),
      NumberType | BooleanType,
      "-T--")
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[Turtle]
      val target = args(0).getAgent.asInstanceOf[Turtle]
      val graph = getGraph(context).asJungGraph
      val distance = Option(graph.dijkstraShortestPath.getDistance(source, target))
      toLogoObject(distance.getOrElse(false))
    }
  }

  object WeightedDistanceToPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType, StringType),
      NumberType | BooleanType,
      "-T--")
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[Turtle]
      val target = args(0).getAgent.asInstanceOf[Turtle]
      val weightVariable = args(1).getString.toUpperCase
      val graph = getGraph(context).asJungGraph
      val distance = Option(graph.dijkstraShortestPath(weightVariable).getDistance(source, target))
      toLogoObject(distance.getOrElse(false))
    }
  }

  object WeightedPathToPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType, StringType),
      NumberType | BooleanType,
      "-T--")
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[Turtle]
      val target = args(0).getAgent.asInstanceOf[Turtle]
      val weightVariable = args(1).getString.toUpperCase
      LogoList.fromJava(
        getGraph(context).asJungGraph
          .dijkstraShortestPath(weightVariable)
          .getPath(source, target))
    }
  }

  object BarabasiAlbertGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandTaskType))
    override def perform(args: Array[Argument], context: Context) {
      val newTurtles = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .barabasiAlbert(args(2).getIntValue, context.getRNG)
      runCommandTaskForTurtles(newTurtles, args(3), context)
    }
  }

  object ErdosRenyiGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, NumberType, CommandTaskType))
    override def perform(args: Array[Argument], context: Context) {
      val newTurtles = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .erdosRenyi(
          nbVertices = args(2).getIntValue,
          connexionProbability = args(3).getDoubleValue,
          rng = context.getRNG)
      runCommandTaskForTurtles(newTurtles, args(4), context)
    }
  }

  object KleinbergSmallWorldGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, NumberType, NumberType, BooleanType, CommandTaskType))
    override def perform(args: Array[Argument], context: Context) {
      val newTurtles = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .kleinbergSmallWorld(
          rowCount = args(2).getIntValue,
          colCount = args(3).getIntValue,
          clusteringExponent = args(4).getDoubleValue,
          isToroidal = args(5).getBooleanValue,
          rng = context.getRNG)
      runCommandTaskForTurtles(newTurtles, args(6), context)
    }
  }

  object Lattice2DGeneratorPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, NumberType, BooleanType, CommandTaskType))
    override def perform(args: Array[Argument], context: Context) {
      val newTurtles = new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .lattice2D(
          rowCount = args(2).getIntValue,
          colCount = args(3).getIntValue,
          isToroidal = args(4).getBooleanValue,
          rng = context.getRNG)
      runCommandTaskForTurtles(newTurtles, args(5), context)
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
        linkBreed = args(2).getAgentSet.requireLinkBreed,
        rng = context.getRNG)
    }
  }

  abstract class InRadiusPrim extends DefaultReporter {
    val edgeType: KNeighborhoodFilter.EdgeType
    override def getSyntax = reporterSyntax(
      Array(NumberType),
      TurtlesetType,
      "-T--")
    override def report(args: Array[Argument], context: Context) = {
      val graph = getGraph(context).asJungGraph
      val source = context.getAgent.asInstanceOf[Turtle]
      val radius = args(0).getIntValue
      if (radius < 0) throw new ExtensionException("radius cannot be negative")
      graph.kNeighborhood(source, radius, edgeType)
    }
  }

  object TurtlesInRadiusPrim extends InRadiusPrim {
    override val edgeType = KNeighborhoodFilter.EdgeType.IN_OUT
  }
  object TurtlesInInRadiusPrim extends InRadiusPrim {
    override val edgeType = KNeighborhoodFilter.EdgeType.IN
  }
  object TurtlesInOutRadiusPrim extends InRadiusPrim {
    override val edgeType = KNeighborhoodFilter.EdgeType.OUT
  }
}