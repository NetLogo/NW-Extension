package org.nlogo.extensions.nw.jung

import scala.collection.JavaConverters.asScalaBufferConverter
import org.nlogo.api
import org.nlogo.nvm
import org.nlogo.agent
import api.DefaultCommand
import api.DefaultReporter
import api.ExtensionException
import api.ScalaConversions._
import api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil._
import org.nlogo.extensions.nw.NetworkExtension
import org.nlogo.extensions.nw.StaticNetLogoGraph

import edu.uci.ics.jung.algorithms.filters.KNeighborhoodFilter
trait Primitives {
  self: NetworkExtension =>

  object KMeansClusters extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(NumberType, NumberType, NumberType),
      ListType)
    override def report(args: Array[api.Argument], context: api.Context) =
      toLogoList(getGraph(context).asJungGraph
        .KMeansClusterer
        .clusters(
          nbClusters = args(0).getIntValue,
          maxIterations = args(1).getIntValue,
          convergenceThreshold = args(2).getDoubleValue,
          rng = context.getRNG))
  }

  object BicomponentClusters extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[api.Argument], context: api.Context) =
      toLogoList(getGraph(context).asUndirectedJungGraph
        .BicomponentClusterer
        .clusters)
  }

  object WeakComponentClusters extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[api.Argument], context: api.Context) =
      toLogoList(getGraph(context).asJungGraph
        .WeakComponentClusterer
        .clusters)
  }

  object BetweennessCentralityPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType, "-T-L")
    override def report(args: Array[api.Argument], context: api.Context) =
      Double.box(getGraph(context).asJungGraph
        .BetweennessCentrality
        .get(context.getAgent))
  }

  object EigenvectorCentralityPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType, "-T--")
    override def report(args: Array[api.Argument], context: api.Context) = {
      val g = getGraph(context).asUndirectedJungGraph
      // make sure graph is connected
      if (g.isWeaklyConnected) // TODO: Actually, it should be STRONGLY connected
        g.EigenvectorCentrality
          .getScore(TurtleToNetLogoTurtle(context.getAgent.asInstanceOf[api.Turtle]))
      else
        java.lang.Boolean.FALSE
    }
  }

  object ClosenessCentralityPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType, "-T--")
    override def report(args: Array[api.Argument], context: api.Context) =
      getGraph(context).asJungGraph
        .ClosenessCentrality
        .getScore(context.getAgent.asInstanceOf[api.Turtle])
  }

  object PathToPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType),
      ListType,
      "-T--")
    override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[api.Turtle]
      val target = args(0).getAgent.asInstanceOf[api.Turtle]
      api.LogoList.fromJava(
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
    override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[api.Turtle]
      val target = args(0).getAgent.asInstanceOf[api.Turtle]
      val path =
        if (source == target)
          Vector(source)
        else {
          val graph = getGraph(context).asJungGraph
          val linkPath = graph.dijkstraShortestPath.getPath(source, target)
          linkPathToTurtlePath(source, linkPath)
        }
      api.LogoList.fromVector(path)
    }
  }

  object TurtlesOnWeightedPathToPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(
      Array(TurtleType, StringType),
      ListType,
      "-T--")
    override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[api.Turtle]
      val target = args(0).getAgent.asInstanceOf[api.Turtle]

      val path =
        if (source == target)
          Vector(source)
        else {
          val graph = getGraph(context).asJungGraph
          val weightVariable = args(1).getString.toUpperCase
          val linkPath = graph.dijkstraShortestPath(weightVariable).getPath(source, target)
          linkPathToTurtlePath(source, linkPath)
        }
      api.LogoList.fromVector(path)
    }
  }

  object MeanPathLengthPrim extends DefaultReporter {
    override def getSyntax = reporterSyntax(NumberType | BooleanType)
    override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
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
    override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
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
    override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[api.Turtle]
      val target = args(0).getAgent.asInstanceOf[api.Turtle]
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
    override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[api.Turtle]
      val target = args(0).getAgent.asInstanceOf[api.Turtle]
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
    override def report(args: Array[api.Argument], context: api.Context): AnyRef = {
      val source = context.getAgent.asInstanceOf[api.Turtle]
      val target = args(0).getAgent.asInstanceOf[api.Turtle]
      val weightVariable = args(1).getString.toUpperCase
      api.LogoList.fromJava(
        getGraph(context).asJungGraph
          .dijkstraShortestPath(weightVariable)
          .getPath(source, target))
    }
  }

  object BarabasiAlbertGeneratorPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .barabasiAlbert(getIntValueWithMinimum(args(2), 1), context.getRNG)
  }

  object ErdosRenyiGeneratorPrim extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType, NumberType, NumberType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      new Generator(
        turtleBreed = args(0).getAgentSet.requireTurtleBreed,
        linkBreed = args(1).getAgentSet.requireLinkBreed)
        .erdosRenyi(
          nbVertices = getIntValueWithMinimum(args(2), 1),
          connexionProbability = args(3).getDoubleValue,
          rng = context.getRNG)
  }

  object KleinbergSmallWorldGeneratorPrim extends turtleCreatingCommand {
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

  object Lattice2DGeneratorPrim extends turtleCreatingCommand {
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

  object SaveMatrix extends api.DefaultCommand {
    override def getSyntax = commandSyntax(Array(StringType))
    override def perform(args: Array[api.Argument], context: api.Context) {
      Matrix.save(getGraph(context).asJungGraph, args(0).getString)
    }
  }

  object LoadMatrix extends turtleCreatingCommand {
    override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType))
    def createTurtles(args: Array[api.Argument], context: api.Context) =
      Matrix.load(
        filename = args(0).getString,
        turtleBreed = args(1).getAgentSet.requireTurtleBreed,
        linkBreed = args(2).getAgentSet.requireLinkBreed,
        rng = context.getRNG)
  }

  object SaveGraphML extends api.DefaultCommand {
    override def getSyntax = commandSyntax(Array(StringType))
    override def perform(args: Array[api.Argument], context: api.Context) {
      GraphML.save(getGraph(context), args(0).getString)
    }
  }

  abstract class InRadiusPrim extends DefaultReporter {
    val edgeType: KNeighborhoodFilter.EdgeType
    override def getSyntax = reporterSyntax(
      Array(NumberType),
      TurtlesetType,
      "-T--")
    override def report(args: Array[api.Argument], context: api.Context) = {
      val graph = getGraph(context).asJungGraph
      val source = context.getAgent.asInstanceOf[api.Turtle]
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