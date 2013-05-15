// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import scala.collection.JavaConverters._

import org.nlogo.api
import org.nlogo.api.{
  Argument,
  Context,
  DefaultCommand
}
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.{
  AgentSetToNetLogoAgentSet,
  AgentSetToRichAgentSet
}

class NetworkExtension extends api.DefaultClassManager {

  override def additionalJars = Seq(
    "collections-generic-4.01.jar",
    "colt-1.2.0.jar",
    "concurrent-1.3.4.jar",
    "jgrapht-jdk1.6-0.8.3.jar",
    "jung-algorithms-2.0.1",
    "jung-api-2.0.1.jar",
    "jung-graph-impl-2.0.1.jar",
    "jung-io-2.0.1.jar",
    "stax-api-1.0.1.jar",
    "wstx-asl-3.2.6.jar").asJava

  // TODO: this is a temporary hack. When we modify
  // the core netlogo, we are going to have
  // set-context and with-context primitives,
  // and the static graph is going to be recomputed
  // only if it is dirty
  private var _graph: Option[NetLogoGraph] = None
  val setGraph: NetLogoGraph => Unit = { g => _graph = Some(g) }
  def getGraph(context: api.Context) = _graph match {
    case Some(g: NetLogoGraph) => g
    case _ =>
      val w = context.getAgent.world
      val g = new StaticNetLogoGraph(w.links, w.turtles)
      _graph = Some(g)
      g
  }

  override def load(primManager: api.PrimitiveManager) {

    val add = primManager.addPrimitive _

    add("set-snapshot", new prim.SetSnapshot(setGraph))

    add("turtles-in-radius", new prim.jung.TurtlesInRadius(getGraph))
    add("turtles-in-out-radius", new prim.jung.TurtlesInOutRadius(getGraph))
    add("turtles-in-in-radius", new prim.jung.TurtlesInInRadius(getGraph))

    add("mean-path-length", new prim.jung.MeanPathLength(getGraph))
    add("mean-weighted-path-length", new prim.jung.MeanWeightedPathLength(getGraph))

    add("distance-to", new prim.jung.DistanceTo(getGraph))
    add("weighted-distance-to", new prim.jung.WeightedDistanceTo(getGraph))
    add("path-to", new prim.jung.PathTo(getGraph))
    add("weighted-path-to", new prim.jung.WeightedPathTo(getGraph))
    add("turtles-on-path-to", new prim.jung.TurtlesOnPathTo(getGraph))
    add("turtles-on-weighted-path-to", new prim.jung.TurtlesOnWeightedPathTo(getGraph))

    add("betweenness-centrality", new prim.jung.BetweennessCentrality(getGraph))
    add("eigenvector-centrality", new prim.jung.EigenvectorCentrality(getGraph))
    add("closeness-centrality", new prim.jung.ClosenessCentrality(getGraph))

    add("bicomponent-clusters", new prim.jung.BicomponentClusters(getGraph))
    add("weak-component-clusters", new prim.jung.WeakComponentClusters(getGraph))

    add("maximal-cliques", new prim.jgrapht.MaximalCliques(getGraph))
    add("biggest-maximal-cliques", new prim.jgrapht.BiggestMaximalCliques(getGraph))

    add("generate-preferential-attachment", new prim.jung.BarabasiAlbertGenerator)
    add("generate-random", new prim.ErdosRenyiGenerator)
    add("generate-small-world", new prim.jung.KleinbergSmallWorldGenerator)
    add("generate-lattice-2d", new prim.jung.Lattice2DGenerator)

    add("generate-ring", new prim.jgrapht.RingGenerator)
    add("generate-star", new prim.jgrapht.StarGenerator)
    add("generate-wheel", new prim.jgrapht.WheelGenerator)
    add("generate-wheel-inward", new prim.jgrapht.WheelGeneratorInward)
    add("generate-wheel-outward", new prim.jgrapht.WheelGeneratorOutward)

    add("save-matrix", new prim.jung.SaveMatrix(getGraph))
    add("load-matrix", new prim.jung.LoadMatrix)

    add("save-graphml", new prim.jung.SaveGraphML(getGraph))
    add("load-graphml", new prim.jung.LoadGraphML)

  }
}
