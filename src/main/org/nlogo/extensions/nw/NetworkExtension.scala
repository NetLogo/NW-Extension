// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import scala.collection.JavaConverters._
import org.nlogo.agent
import org.nlogo.api

class NetworkExtension extends api.DefaultClassManager {

  val version = "1.0.0-RC3"

  def checkNetLogoVersion(): Unit = {
    try {
      Class.forName("org.nlogo.api.SimpleChangeEventPublisher")
    } catch {
      case e: ClassNotFoundException => throw new api.ExtensionException(
        "Version " + version + " of the NW extension requires NetLogo version 5.0.5 or greater.", e)
    }
  }

  override def additionalJars = Seq(
    "collections-generic-4.01.jar",
    "colt-1.2.0.jar",
    "concurrent-1.3.4.jar",
    "jgrapht-jdk1.6-0.8.3.jar",
    "jung-algorithms-2.0.1.jar",
    "jung-api-2.0.1.jar",
    "jung-graph-impl-2.0.1.jar",
    "jung-io-2.0.1.jar",
    "stax-api-1.0.1.jar",
    "wstx-asl-3.2.6.jar").asJava

  private var _graphContext: Option[GraphContext] = None
  val setGraphContext: GraphContext => Unit = { gc => _graphContext = Some(gc) }
  def getGraphContext(world: api.World) =
    _graphContext.getOrElse {
      val w = world.asInstanceOf[agent.World]
      val gc = new GraphContext(w, w.turtles, w.links)
      _graphContext = Some(gc)
      gc
    }

  override def clearAll() { _graphContext = None }
  override def unload(em: api.ExtensionManager) { _graphContext = None }

  override def load(primManager: api.PrimitiveManager) {

    checkNetLogoVersion()

    val add = primManager.addPrimitive _

    add("version", new prim.Version(this))

    add("set-context", new prim.SetContext(setGraphContext))
    add("get-context", new prim.GetContext(getGraphContext))

    add("turtles-in-radius", new org.nlogo.extensions.nw.prim.TurtlesInRadius(getGraphContext))
    add("turtles-in-reverse-radius", new org.nlogo.extensions.nw.prim.TurtlesInReverseRadius(getGraphContext))

    add("mean-path-length", new prim.MeanPathLength(getGraphContext))
    add("mean-weighted-path-length", new prim.jung.MeanWeightedPathLength(getGraphContext))

    add("distance-to", new prim.DistanceTo(getGraphContext))
    add("weighted-distance-to", new prim.jung.WeightedDistanceTo(getGraphContext))
    add("path-to", new prim.PathTo(getGraphContext))
    add("weighted-path-to", new prim.jung.WeightedPathTo(getGraphContext))
    add("turtles-on-path-to", new prim.TurtlesOnPathTo(getGraphContext))
    add("turtles-on-weighted-path-to", new prim.jung.TurtlesOnWeightedPathTo(getGraphContext))

    add("betweenness-centrality", new prim.jung.BetweennessCentrality(getGraphContext))
    add("eigenvector-centrality", new prim.jung.EigenvectorCentrality(getGraphContext))
    add("closeness-centrality", new prim.jung.ClosenessCentrality(getGraphContext))

    add("bicomponent-clusters", new prim.jung.BicomponentClusters(getGraphContext))
    add("weak-component-clusters", new prim.jung.WeakComponentClusters(getGraphContext))

    add("maximal-cliques", new prim.jgrapht.MaximalCliques(getGraphContext))
    add("biggest-maximal-cliques", new prim.jgrapht.BiggestMaximalCliques(getGraphContext))

    add("generate-preferential-attachment", new prim.jung.BarabasiAlbertGenerator)
    add("generate-random", new prim.ErdosRenyiGenerator)
    add("generate-small-world", new prim.jung.KleinbergSmallWorldGenerator)
    add("generate-lattice-2d", new prim.jung.Lattice2DGenerator)

    add("generate-ring", new prim.jgrapht.RingGenerator)
    add("generate-star", new prim.jgrapht.StarGenerator)
    add("generate-wheel", new prim.jgrapht.WheelGenerator)
    add("generate-wheel-inward", new prim.jgrapht.WheelGeneratorInward)
    add("generate-wheel-outward", new prim.jgrapht.WheelGeneratorOutward)

    add("save-matrix", new prim.jung.SaveMatrix(getGraphContext))
    add("load-matrix", new prim.jung.LoadMatrix)

    add("save-graphml", new prim.jung.SaveGraphML(getGraphContext))
    add("load-graphml", new prim.jung.LoadGraphML)

  }
}
