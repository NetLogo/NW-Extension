// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import scala.collection.JavaConverters.seqAsJavaListConverter

import org.nlogo.api.Syntax.AgentsetType
import org.nlogo.api.Syntax.commandSyntax
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.DefaultClassManager
import org.nlogo.api.DefaultCommand
import org.nlogo.api.PrimitiveManager
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToNetLogoAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet

// TODO: program everything against the API, if possible

class NetworkExtension extends DefaultClassManager
  with HasGraph
  with jung.Primitives
  with jgrapht.Primitives {

  override def additionalJars = Seq(
    "collections-generic-4.01.jar",
    "colt-1.2.0.jar",
    "concurrent-1.3.4.jar",
    "jgrapht-jdk1.6-0.8.3.jar",
    "jung-algorithms-2.0.2-nlfork-0.1.jar",
    "jung-api-2.0.1.jar",
    "jung-graph-impl-2.0.1.jar",
    "jung-io-2.0.1.jar",
    "stax-api-1.0.1.jar",
    "wstx-asl-3.2.6.jar").asJava

  override def load(primManager: PrimitiveManager) {

    val add = primManager.addPrimitive _

    add("set-snapshot", SnapshotPrim)

    add("turtles-in-radius", TurtlesInRadiusPrim)
    add("turtles-in-out-radius", TurtlesInOutRadiusPrim)
    add("turtles-in-in-radius", TurtlesInInRadiusPrim)

    add("mean-path-length", MeanPathLengthPrim)
    add("mean-weighted-path-length", MeanWeightedPathLengthPrim)

    add("distance-to", DistanceToPrim)
    add("weighted-distance-to", WeightedDistanceToPrim)
    add("path-to", PathToPrim)
    add("weighted-path-to", WeightedPathToPrim)
    add("turtles-on-path-to", TurtlesOnPathToPrim)
    add("turtles-on-weighted-path-to", TurtlesOnWeightedPathToPrim)

    add("betweenness-centrality", BetweennessCentralityPrim)
    add("eigenvector-centrality", EigenvectorCentralityPrim)
    add("closeness-centrality", ClosenessCentralityPrim)

    add("k-means-clusters", KMeansClusters)
    add("bicomponent-clusters", BicomponentClusters)
    add("weak-component-clusters", WeakComponentClusters)

    add("maximal-cliques", MaximalCliques)
    add("biggest-maximal-clique", BiggestMaximalClique)

    add("generate-preferential-attachment", BarabasiAlbertGeneratorPrim)
    add("generate-random", ErdosRenyiGeneratorPrim)
    add("generate-small-world", KleinbergSmallWorldGeneratorPrim)
    add("generate-lattice-2d", Lattice2DGeneratorPrim)
    add("generate-ring", RingGeneratorPrim)
    add("generate-star", StarGeneratorPrim)
    add("generate-wheel", WheelGeneratorPrim)
    add("generate-wheel-inward", WheelGeneratorInwardPrim)
    add("generate-wheel-outward", WheelGeneratorOutwardPrim)

    add("save-matrix", SaveMatrix)
    add("load-matrix", LoadMatrix)

    add("save-graphml", SaveGraphML)

  }
}

trait HasGraph {
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

  object SnapshotPrim extends DefaultCommand {
    override def getSyntax = commandSyntax(
      Array(AgentsetType, AgentsetType))
    override def perform(args: Array[Argument], context: Context) {
      val turtleSet = args(0).getAgentSet.requireTurtleSet
      val linkSet = args(1).getAgentSet.requireLinkSet
      setGraph(new StaticNetLogoGraph(linkSet, turtleSet))
    }
  }
}