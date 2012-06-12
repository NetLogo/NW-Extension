package org.nlogo.extensions.nw.jgrapht

import org.nlogo.agent.AgentSet
import org.nlogo.agent.Turtle
import org.nlogo.agent.Link
import org.jgrapht
import org.jgrapht.generate.RingGraphGenerator
import org.jgrapht.generate.WheelGraphGenerator
import org.jgrapht.generate.StarGraphGenerator
import org.jgrapht.VertexFactory
import scala.collection.JavaConverters._
import org.jgrapht.generate.GraphGenerator

class Vertex
class Edge
class Generator(
  turtleBreed: AgentSet,
  linkBreed: AgentSet) {

  private lazy val vertexFactory = new VertexFactory[Vertex] { def createVertex = new Vertex }

  private def newGraph =
    if (linkBreed.isDirected)
      new jgrapht.graph.SimpleDirectedGraph[Vertex, Edge](classOf[Edge])
    else
      new jgrapht.graph.SimpleGraph[Vertex, Edge](classOf[Edge])

  private def importToNetLogo(graph: org.jgrapht.Graph[Vertex, Edge]) = {
    val w = turtleBreed.world
    val m = graph.vertexSet.asScala.map { v =>
      v -> turtleBreed.world.createTurtle(
        turtleBreed,
        w.mainRNG.nextInt(14), // color
        w.mainRNG.nextInt(360)) // heading
    }.toMap
    graph.edgeSet.asScala
      .foreach { edge =>
        w.linkManager.createLink(
          m(graph.getEdgeSource(edge)),
          m(graph.getEdgeTarget(edge)),
          linkBreed)
      }
    m.valuesIterator // return turtles
  }

  private def resultMap[T] = new java.util.HashMap[String, T]()

  private def generate[T](generator: GraphGenerator[Vertex, Edge, T]) = {
    val g = newGraph
    generator.generateGraph(g, vertexFactory, resultMap[T])
    importToNetLogo(g)
  }

  def ringGraphGenerator(size: Int) =
    generate[Vertex](new RingGraphGenerator(size))

  def wheelGraphGenerator(size: Int, inwardSpokes: Boolean) =
    generate[Vertex](new WheelGraphGenerator(size, inwardSpokes))

  def starGraphGenerator(size: Int) =
    generate[Vertex](new StarGraphGenerator(size))

}