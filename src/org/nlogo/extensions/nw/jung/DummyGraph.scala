package org.nlogo.extensions.nw.jung

import edu.uci.ics.jung
import org.apache.commons.collections15.Factory
import org.nlogo.agent.AgentSet
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import java.util.Random

object DummyGraph {
  // TODO: the vertex id thing is a ugly hack to get around the fact that 
  // Jung has no ordered SparseGraph (only ordered MultiGraphs). Ideally, 
  // we would add a custom sorted graph. NP 2012-06-13
  private var vertexIdCounter = 0L
  case class Vertex(val id: Long)
  class Edge
  def edgeFactory: Factory[Edge] = new Factory[Edge]() {
    def create = new Edge
  }
  def vertexFactory: Factory[Vertex] = new Factory[Vertex]() {
    def create = {
      vertexIdCounter += 1
      new Vertex(vertexIdCounter)
    }
  }

  def factoryFor(linkBreed: AgentSet) =
    if (linkBreed.isDirected)
      new Factory[jung.graph.Graph[Vertex, Edge]]() {
        def create = new jung.graph.DirectedSparseGraph[Vertex, Edge]
      }
    else
      new Factory[jung.graph.Graph[Vertex, Edge]]() {
        def create = new jung.graph.UndirectedSparseGraph[Vertex, Edge]
      }

  def directedFactory = jung.graph.DirectedSparseGraph.getFactory[Vertex, Edge]
  def undirectedFactory = jung.graph.UndirectedSparseGraph.getFactory[Vertex, Edge]

  def importToNetLogo(
    graph: jung.graph.Graph[Vertex, Edge],
    turtleBreed: AgentSet,
    linkBreed: AgentSet,
    rng: Random,
    sorted: Boolean = false) = {
    val w = turtleBreed.world
    val vs = graph.getVertices.asScala
    val m = (if (sorted) vs.toSeq.sortBy(_.id) else vs).map { v =>
      v -> turtleBreed.world.createTurtle(
        turtleBreed,
        rng.nextInt(14), // color
        rng.nextInt(360)) // heading
    }.toMap
    graph.getEdges.asScala
      .map(graph.getEndpoints(_))
      .foreach { endPoints =>
        w.linkManager.createLink(
          m(endPoints.getFirst),
          m(endPoints.getSecond),
          linkBreed)
      }
    m.valuesIterator // return turtles
  }
}