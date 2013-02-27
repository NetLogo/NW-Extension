// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jgrapht

import java.lang.IllegalArgumentException

import scala.collection.JavaConverters._

import org.nlogo.agent.{ Link, Turtle }
import org.nlogo.api
import org.nlogo.api.ExtensionException
import org.nlogo.extensions.nw.NetLogoGraph
import org.nlogo.extensions.nw.util.TurtleSetsConverters.{ emptyTurtleSet, toTurtleSets }
import org.nlogo.util.MersenneTwisterFast

import org.jgrapht

trait Graph
  extends jgrapht.graph.AbstractGraph[Turtle, Link] {
  val nlg: NetLogoGraph

  override def getAllEdges(sourceVertex: Turtle, targetVertex: Turtle) =
    (for {
      src <- nlg.validTurtle(sourceVertex)
      tgt <- nlg.validTurtle(targetVertex)
      edges = nlg.allEdges(src).toSet intersect nlg.allEdges(tgt).toSet
    } yield edges.asJava).orNull

  override def getEdge(sourceVertex: Turtle, targetVertex: Turtle) =
    if (nlg.isDirected)
      nlg.directedOutEdges(sourceVertex).find(_.end2 == targetVertex).orNull
    else
      nlg.allEdges(sourceVertex).find(l => l.end1 == targetVertex || l.end2 == targetVertex).orNull

  override def containsEdge(sourceVertex: Turtle, targetVertex: Turtle) =
    getEdge(sourceVertex, targetVertex) != null

  override def containsEdge(edge: Link) = nlg.links.exists(_ == edge)
  override def containsVertex(vertex: Turtle) = nlg.turtles.exists(_ == vertex)

  override def edgeSet() = nlg.links.toSet.asJava

  override def edgesOf(vertex: Turtle) =
    nlg.validTurtle(vertex)
      .map(nlg.allEdges(_).toSet.asJava)
      .getOrElse(throw new IllegalArgumentException("turtle not in graph"))

  override def vertexSet() = nlg.turtles.toSet.asJava

  override def getEdgeSource(edge: Link) = edge.end1
  override def getEdgeTarget(edge: Link) = edge.end2
  override def getEdgeWeight(edge: Link): Double = 1.0 // TODO: figure out how to deal with weigths

  override def getEdgeFactory() = throw sys.error("not implemented")
  override def addEdge(sourceVertex: Turtle, targetVertex: Turtle) = throw sys.error("not implemented")
  override def addEdge(sourceVertex: Turtle, targetVertex: Turtle, edge: Link) = throw sys.error("not implemented")
  override def addVertex(v: Turtle) = throw sys.error("not implemented")
  override def removeAllEdges(edges: java.util.Collection[_ <: Link]) = throw sys.error("not implemented")
  override def removeAllEdges(sourceVertex: Turtle, targetVertex: Turtle) = throw sys.error("not implemented")
  override def removeAllVertices(vertices: java.util.Collection[_ <: Turtle]) = throw sys.error("not implemented")
  override def removeEdge(sourceVertex: Turtle, targetVertex: Turtle) = throw sys.error("not implemented")
  override def removeEdge(edge: Link) = throw sys.error("not implemented")
  override def removeVertex(vertex: Turtle) = throw sys.error("not implemented")

  object BronKerboschCliqueFinder extends jgrapht.alg.BronKerboschCliqueFinder(this) {
    def allCliques: Seq[api.AgentSet] =
      toTurtleSets(getAllMaximalCliques.asScala, nlg.world)
    def biggestCliques: Seq[api.AgentSet] =
      toTurtleSets(getBiggestMaximalCliques.asScala, nlg.world)
    def biggestClique(rng: MersenneTwisterFast): api.AgentSet = {
      val cliques = biggestCliques
      if (cliques.isEmpty)
        emptyTurtleSet(nlg.world)
      else
        cliques(rng.nextInt(cliques.size))
    }
  }
}

class UndirectedGraph(
  override val nlg: NetLogoGraph)
  extends Graph
  with jgrapht.UndirectedGraph[Turtle, Link] {
  override def degreeOf(vertex: Turtle) = nlg.allEdges(vertex).size
}

class DirectedGraph(
  override val nlg: NetLogoGraph)
  extends Graph
  with jgrapht.DirectedGraph[Turtle, Link] {
  if (!nlg.isDirected)
    throw new ExtensionException("link set must be directed")

  override def incomingEdgesOf(vertex: Turtle) = nlg.directedInEdges(vertex).toSet.asJava
  override def inDegreeOf(vertex: Turtle) = nlg.directedInEdges(vertex).size
  override def outgoingEdgesOf(vertex: Turtle) = nlg.directedOutEdges(vertex).toSet.asJava
  override def outDegreeOf(vertex: Turtle) = nlg.directedOutEdges(vertex).size

}