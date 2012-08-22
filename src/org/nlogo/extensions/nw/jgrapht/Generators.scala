package org.nlogo.extensions.nw.jgrapht

import scala.collection.JavaConverters.asScalaSetConverter
import org.jgrapht.generate.GraphGenerator
import org.jgrapht.generate.RingGraphGenerator
import org.jgrapht.generate.StarGraphGenerator
import org.jgrapht.generate.WheelGraphGenerator
import org.jgrapht.VertexFactory
import org.nlogo.agent.AgentSet
import org.jgrapht
import java.util.Random

class Vertex
class Edge
class Generator(
  turtleBreed: AgentSet,
  linkBreed: AgentSet) {

  private object vertexFactory extends VertexFactory[Vertex] {
    override def createVertex = new Vertex
  }

  private def newGraph =
    if (linkBreed.isDirected)
      new jgrapht.graph.SimpleDirectedGraph[Vertex, Edge](classOf[Edge])
    else
      new jgrapht.graph.SimpleGraph[Vertex, Edge](classOf[Edge])

  private def importToNetLogo(graph: org.jgrapht.Graph[Vertex, Edge], rng: Random) = {
    val w = turtleBreed.world
    val m = asScalaSetConverter(graph.vertexSet).asScala.map { v =>
      v -> turtleBreed.world.createTurtle(
        turtleBreed,
        rng.nextInt(14), // color
        rng.nextInt(360)) // heading
    }.toMap
    asScalaSetConverter(graph.edgeSet).asScala
      .foreach { edge =>
        w.linkManager.createLink(
          m(graph.getEdgeSource(edge)),
          m(graph.getEdgeTarget(edge)),
          linkBreed)
      }
    m.valuesIterator // return turtles
  }

  private def resultMap[T] = new java.util.HashMap[String, T]()

  private def generate[T](generator: GraphGenerator[Vertex, Edge, T], rng: Random) = {
    val g = newGraph
    generator.generateGraph(g, vertexFactory, resultMap[T])
    importToNetLogo(g, rng)
  }

  def ringGraphGenerator(size: Int, rng: Random) =
    generate[Vertex](new RingGraphGenerator(size), rng)

  def wheelGraphGenerator(size: Int, inwardSpokes: Boolean, rng: Random) =
    generate[Vertex](new WheelGraphGenerator(size, inwardSpokes), rng)

  def starGraphGenerator(size: Int, rng: Random) =
    generate[Vertex](new StarGraphGenerator(size), rng)

}