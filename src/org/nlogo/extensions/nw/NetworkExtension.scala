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
import NetworkExtension._

// TODO: program everything against the API, if possible

class NetworkExtension extends DefaultClassManager
  with HasSnapshot
  with NetworkPrimitives {

  override def load(primManager: PrimitiveManager) {
    val add = primManager.addPrimitive _
    add("link-distance", LinkDistance)
    add("link-path", LinkPath)
    add("set-snapshot", Snapshot)
    add("betweenness-centrality", BetweennessCentralityPrim)
    add("eigenvector-centrality", EigenvectorCentralityPrim)
    add("closeness-centrality", ClosenessCentralityPrim)
    add("k-means-clusters", KMeansClusters)
    add("bicomponent-clusters", BicomponentClusters)
    add("weak-component-clusters", WeakComponentClusters)
    add("generate-power-law", EppsteinPowerLawGeneratorPrim)
    add("generate-preferential-attachment", BarabasiAlbertGeneratorPrim)
    add("generate-random", ErdosRenyiGeneratorPrim)
    add("generate-small-world", KleinbergSmallWorldGeneratorPrim)
    add("generate-lattice-2d", Lattice2DGeneratorPrim)
    add("save-matrix", SaveMatrix)
    add("load-matrix", LoadMatrix)
  }
}

trait HasSnapshot {
  // TODO: this is a temporary hack. When we modify
  // the core netlogo, we are going to have
  // set-context and with-context primitives,
  // and the static graph is going to be recomputed
  // only if it is dirty
  private var _graph: Option[NetLogoGraph] = None
  def setGraph(g: NetLogoGraph) { _graph = Some(g) }
  def getGraph(context: Context) = _graph match {
    case Some(g: NetLogoGraph) => g
    case _ =>
      val w = context.getAgent.world
      val g = new StaticNetLogoGraph(w.links, w.turtles)
      _graph = Some(g)
      g
  }
}

trait NetworkPrimitives {
  self: NetworkExtension =>
    
  object Snapshot extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(TurtlesetType, LinksetType))
    override def perform(args: Array[Argument], context: Context) {
      val turtleBreed = args(0).getAgentSet.requireTurtleBreed
      val linkBreed = args(1).getAgentSet.requireLinkBreed
      setGraph(new StaticNetLogoGraph(linkBreed, turtleBreed))
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

  object SaveMatrix extends DefaultCommand {
    override def getSyntax = commandSyntax(Array(StringType))
    override def perform(args: Array[Argument], context: Context) {
      JungMatrix.save(getGraph(context).asJungGraph, args(0).getString)
    }
  }

  object LoadMatrix extends DefaultCommand {
    override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType))
    override def perform(args: Array[Argument], context: Context) {
      JungMatrix.load(
        filename = args(0).getString,
        turtleBreed = args(1).getAgentSet.requireTurtleBreed,
        linkBreed = args(2).getAgentSet.requireLinkBreed)
    }
  }

}