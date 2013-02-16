// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jung

import java.util.Random

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

import org.apache.commons.collections15.Factory
import org.nlogo.agent.{ AgentSet, Turtle }

import edu.uci.ics.jung

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

  def importToNetLogo(
    graph: jung.graph.Graph[Vertex, Edge],
    turtleBreed: AgentSet,
    linkBreed: AgentSet,
    rng: Random,
    sorted: Boolean = false) = {
    val w = turtleBreed.world

    val vs = graph.getVertices.asScala
    val vertices = if (sorted) vs.toSeq.sortBy(_.id) else vs

    val turtles: Map[Vertex, Turtle] =
      vertices.map { v =>
        v -> createTurtle(turtleBreed, rng)
      }(collection.breakOut)

    graph.getEdges.asScala.foreach { e =>
      createLink(turtles, graph.getEndpoints(e), linkBreed)
    }

    turtles.valuesIterator
  }
}