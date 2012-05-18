package org.nlogo.extensions.nw

import org.apache.commons.collections15.Factory
import edu.uci.ics.jung.graph.Graph
import edu.uci.ics.jung.graph.SparseGraph
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.agent.World
import edu.uci.ics.jung.io.MatrixFile
import org.nlogo.agent.AgentSet
import scala.collection.JavaConverters._
import edu.uci.ics.jung.graph.UndirectedGraph
import edu.uci.ics.jung.graph.UndirectedSparseGraph

object DummyGraph {
  class Vertex
  class Edge
  def edgeFactory: Factory[Edge] = new Factory[Edge]() {
    def create = new Edge
  }
  def vertexFactory: Factory[Vertex] = new Factory[Vertex]() {
    def create = new Vertex
  }
  def factory: Factory[Graph[Vertex, Edge]] =
    new Factory[Graph[Vertex, Edge]]() {
      def create = new SparseGraph[Vertex, Edge]
    }
  def undirectedFactory: Factory[UndirectedGraph[Vertex, Edge]] =
    new Factory[UndirectedGraph[Vertex, Edge]]() {
      def create = new UndirectedSparseGraph[Vertex, Edge]
    }

  def importToNetLogo(graph: Graph[Vertex, Edge], turtleBreed: AgentSet, linkBreed: AgentSet) = {
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