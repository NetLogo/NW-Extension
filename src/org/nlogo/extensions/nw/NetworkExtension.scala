package org.nlogo.extensions.nw

import org.nlogo.api.ScalaConversions.toRichAny
import org.nlogo.api.ScalaConversions.toRichSeq
import org.nlogo.api.Turtle
import org.nlogo.api.Agent
import org.nlogo.api.AgentSet
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.DefaultClassManager
import org.nlogo.api.DefaultCommand
import org.nlogo.api.DefaultReporter
import org.nlogo.api.ExtensionException
import org.nlogo.api.I18N
import org.nlogo.api.Link
import org.nlogo.api.LogoList
import org.nlogo.api.PrimitiveManager
import org.nlogo.api.Syntax._
import org.nlogo.api.TypeNames
import org.nlogo.extensions.nw.NetworkExtensionUtil._

class NetworkExtension extends DefaultClassManager {
  override def load(primManager: PrimitiveManager) {
    val add = primManager.addPrimitive _
    add("link-distance", LinkDistance)
    add("link-path", LinkPath)
    add("snapshot", Snapshot)
    add("betweenness-centrality", BetweennessCentralityPrim)
    add("eigenvector-centrality", EigenvectorCentralityPrim)
    add("closeness-centrality", ClosenessCentralityPrim)
    add("k-means-clusters", KMeansClusters)
    add("bicomponent-clusters", BicomponentClusters)
    add("generate-eppstein-power-law", EppsteinPowerLawGeneratorPrim)
    add("generate-barabasi-albert", BarabasiAlbertGeneratorPrim)
    add("generate-erdos-renyi", ErdosRenyiGeneratorPrim)
    add("generate-kleinberg-small-world", KleinbergSmallWorldGeneratorPrim)
    add("generate-lattice-2d", Lattice2DGeneratorPrim)
  }
}

object Snapshot extends DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtlesetType, LinksetType),
    WildcardType)
  override def report(args: Array[Argument], context: Context): AnyRef = {
    val turtleSet = args(0).getAgentSet
    val linkSet = args(1).getAgentSet
    new StaticNetLogoGraph(linkSet, turtleSet).toLogoObject // make extension type
  }
}

object KMeansClusters extends DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(WildcardType, NumberType, NumberType, NumberType),
    ListType)
  override def report(args: Array[Argument], context: Context) = {
    args(0).getStaticGraph.asJungGraph
      .kMeansClusterer
      .clusters(
        nbClusters = args(1).getIntValue,
        maxIterations = args(2).getIntValue,
        convergenceThreshold = args(3).getDoubleValue)
      .toLogoList
  }
}

object BicomponentClusters extends DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(WildcardType),
    ListType)
  override def report(args: Array[Argument], context: Context) = {
    args(0).getStaticGraph.asUndirectedJungGraph
      .bicomponentClusterer
      .clusters
      .toLogoList
  }
}

object BetweennessCentralityPrim extends DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(WildcardType),
    NumberType,
    agentClassString = "-T-L")
  override def report(args: Array[Argument], context: Context) =
    args(0).getStaticGraph.asJungGraph
      .betweennessCentrality
      .get(context.getAgent)
      .toLogoObject
}

object EigenvectorCentralityPrim extends DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(WildcardType),
    NumberType,
    agentClassString = "-T--")
  override def report(args: Array[Argument], context: Context) =
    args(0).getStaticGraph.asJungGraph
      .eigenvectorCentrality
      .getVertexScore(context.getAgent.asInstanceOf[Turtle])
      .toLogoObject
}

object ClosenessCentralityPrim extends DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(WildcardType),
    NumberType,
    agentClassString = "-T--")
  override def report(args: Array[Argument], context: Context) =
    args(0).getStaticGraph.asJungGraph
      .closenessCentrality
      .getVertexScore(context.getAgent.asInstanceOf[Turtle])
      .toLogoObject
}

object LinkPath extends DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, LinksetType | WildcardType),
    ListType,
    agentClassString = "-T--")
  override def report(args: Array[Argument], context: Context): AnyRef = {
    val start = context.getAgent.asInstanceOf[Turtle]
    val end = args(0).getAgent.asInstanceOf[Turtle]
    val path = args(1).getGraph.asJungGraph.dijkstraShortestPath.getPath(start, end)
    LogoList.fromJava(path)
  }
}

object LinkDistance extends DefaultReporter {
  override def getSyntax = reporterSyntax(
    Array(TurtleType, LinksetType | WildcardType),
    NumberType | BooleanType,
    agentClassString = "-T--")
  override def report(args: Array[Argument], context: Context): AnyRef = {
    val start = context.getAgent.asInstanceOf[Turtle]
    val end = args(0).getAgent.asInstanceOf[Turtle]
    val path = args(1).getGraph.asJungGraph.dijkstraShortestPath.getPath(start, end)
    Option(path.size).filterNot(0==).getOrElse(false).toLogoObject
  }
}

object EppsteinPowerLawGeneratorPrim extends DefaultCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, NumberType, NumberType))
  override def perform(args: Array[Argument], context: Context) {
    new JungGraphGenerator(
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
    new JungGraphGenerator(
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
    Array(TurtlesetType, LinksetType, NumberType, NumberType))
  override def perform(args: Array[Argument], context: Context) {
    new JungGraphGenerator(
      turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      linkBreed = args(1).getAgentSet.requireLinkBreed)
      .erdosRenyi(
        nbVertices = args(2).getIntValue,
        connexionProbability = args(3).getDoubleValue)
  }
}

object KleinbergSmallWorldGeneratorPrim extends DefaultCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, NumberType, NumberType, BooleanType))
  override def perform(args: Array[Argument], context: Context) {
    new JungGraphGenerator(
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
    new JungGraphGenerator(
      turtleBreed = args(0).getAgentSet.requireTurtleBreed,
      linkBreed = args(1).getAgentSet.requireLinkBreed)
      .lattice2D(
        rowCount = args(2).getIntValue,
        colCount = args(3).getIntValue,
        isToroidal = args(4).getBooleanValue)
  }
}
