package org.nlogo.extensions.nw.jung

import edu.uci.ics.jung
import org.apache.commons.collections15.Factory
import org.nlogo.agent.AgentSet
import scala.collection.JavaConverters.collectionAsScalaIterableConverter

object DummyGraph {
  class Vertex
  class Edge
  def edgeFactory: Factory[Edge] = new Factory[Edge]() {
    def create = new Edge
  }
  def vertexFactory: Factory[Vertex] = new Factory[Vertex]() {
    def create = new Vertex
  }
  def factory: Factory[jung.graph.Graph[Vertex, Edge]] =
    new Factory[jung.graph.Graph[Vertex, Edge]]() {
      def create = new jung.graph.SparseGraph[Vertex, Edge]
    }
  def undirectedFactory: Factory[jung.graph.UndirectedGraph[Vertex, Edge]] =
    new Factory[jung.graph.UndirectedGraph[Vertex, Edge]]() {
      def create = new jung.graph.UndirectedSparseGraph[Vertex, Edge]
    }

  def importToNetLogo(
    graph: jung.graph.Graph[Vertex, Edge],
    turtleBreed: AgentSet,
    linkBreed: AgentSet) = {
    val w = turtleBreed.world
    val m = graph.getVertices.asScala.map { v =>
      v -> turtleBreed.world.createTurtle(
        turtleBreed,
        w.mainRNG.nextInt(14), // color
        w.mainRNG.nextInt(360)) // heading
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