package org.nlogo.extensions.nw

import org.nlogo.api.Context
import org.nlogo.api.DefaultClassManager
import org.nlogo.api.PrimitiveManager
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToNetLogoAgentSet

// TODO: program everything against the API, if possible

class NetworkExtension extends DefaultClassManager
  with HasGraph
  with nl.jung.Primitives
  with nl.jgrapht.Primitives {

  override def load(primManager: PrimitiveManager) {
    val add = primManager.addPrimitive _

    // In original extension:
    add("in-link-radius", InLinkRadius)
    add("in-out-link-radius", InOutLinkRadius)
    add("in-in-link-radius", InInLinkRadius)
    add("mean-link-path-length", MeanLinkPathLength)
    add("link-distance", LinkDistance)
    add("link-path", LinkPath)
    
    // New:
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
    add("maximal-cliques", MaximalCliques)
    add("biggest-maximal-clique", BiggestMaximalClique)
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
}

