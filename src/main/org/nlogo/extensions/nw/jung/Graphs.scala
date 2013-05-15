// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jung

import java.util.Collection

import scala.collection.JavaConverters._

import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import org.nlogo.extensions.nw.GraphContext

import edu.uci.ics
import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.graph.util.Pair

trait Graph
  extends ics.jung.graph.AbstractGraph[Turtle, Link]
  with Algorithms {

  val gc: GraphContext

  def edgeType =
    if (gc.isDirected)
      ics.jung.graph.util.EdgeType.DIRECTED
    else
      ics.jung.graph.util.EdgeType.UNDIRECTED

  override def getIncidentEdges(turtle: Turtle): Collection[Link] =
    gc.validTurtle(turtle).map(gc.allEdges(_).asJavaCollection).orNull

  override def getEdgeCount(): Int = gc.linkCount

  override def getNeighbors(turtle: Turtle): Collection[Turtle] =
    gc.validTurtle(turtle).map { t =>
      (gc.outEdges(t).map(_.end2) ++ gc.inEdges(t).map(_.end1)).asJavaCollection
    }.orNull

  override def getVertexCount(): Int = gc.turtleCount
  override def getVertices(): Collection[Turtle] = gc.turtles.asJavaCollection
  override def getEdges(): Collection[Link] = gc.links.asJavaCollection
  override def containsEdge(link: Link): Boolean = gc.isValidLink(link)
  override def containsVertex(turtle: Turtle): Boolean = gc.isValidTurtle(turtle)

  def getEndpoints(link: Link): Pair[Turtle] =
    new Pair(link.end1, link.end2) // Note: contract says nothing about edge being in graph

  def removeEdge(link: Link): Boolean =
    throw sys.error("not implemented")
  def removeVertex(turtle: Turtle): Boolean =
    throw sys.error("not implemented")
  def addVertex(turtle: Turtle): Boolean =
    throw sys.error("not implemented")
  override def addEdge(link: Link, turtles: Collection[_ <: Turtle]): Boolean =
    throw sys.error("not implemented")
  def addEdge(link: Link, turtles: Pair[_ <: Turtle], edgeType: EdgeType): Boolean =
    throw sys.error("not implemented")

  lazy val asSparseGraph: ics.jung.graph.SparseGraph[Turtle, Link] = {
    val g = new ics.jung.graph.SparseGraph[Turtle, Link]()
    gc.turtles.foreach(g.addVertex)
    gc.links.foreach { l =>
      g.addEdge(l, new ics.jung.graph.util.Pair(l.end1, l.end2), edgeType)
    }
    g
  }

  // If the whole graph is only one cluster, then it's weakly connected
  lazy val isWeaklyConnected = WeakComponentClusterer.transform(this).size == 1
}

class DirectedGraph(
  override val gc: GraphContext)
  extends ics.jung.graph.AbstractTypedGraph[Turtle, Link](EdgeType.DIRECTED)
  with Graph
  with DirectedAlgorithms
  with ics.jung.graph.DirectedGraph[Turtle, Link] {

  if (!gc.isDirected)
    throw new ExtensionException("link set must be directed")

  override def getInEdges(turtle: Turtle): Collection[Link] =
    gc.validTurtle(turtle).map(gc.inEdges(_).asJavaCollection).orNull
  override def getPredecessors(turtle: Turtle): Collection[Turtle] =
    gc.validTurtle(turtle).map(gc.inEdges(_).map(_.end1).asJavaCollection).orNull

  override def getOutEdges(turtle: Turtle): Collection[Link] =
    gc.validTurtle(turtle).map(gc.outEdges(_).asJavaCollection).orNull
  override def getSuccessors(turtle: Turtle): Collection[Turtle] =
    gc.validTurtle(turtle).map(gc.outEdges(_).map(_.end2).asJavaCollection).orNull

  def isDest(turtle: Turtle, link: Link): Boolean =
    gc.validLink(link).filter(_.end2 == turtle).isDefined
  def isSource(turtle: Turtle, link: Link): Boolean =
    gc.validLink(link).filter(_.end1 == turtle).isDefined

  override def getSource(link: Link): Turtle =
    gc.validLink(link).map(_.end1).orNull

  override def getDest(link: Link): Turtle =
    gc.validLink(link).map(_.end2).orNull

}

class UndirectedGraph(
  override val gc: GraphContext)
  extends ics.jung.graph.AbstractTypedGraph[Turtle, Link](EdgeType.UNDIRECTED)
  with Graph
  with UndirectedAlgorithms
  with ics.jung.graph.UndirectedGraph[Turtle, Link] {

  override def getInEdges(turtle: Turtle) = getIncidentEdges(turtle)
  override def getPredecessors(turtle: Turtle) = getNeighbors(turtle: Turtle)

  override def getOutEdges(turtle: Turtle) = getIncidentEdges(turtle)
  override def getSuccessors(turtle: Turtle) = getNeighbors(turtle: Turtle)

  def isDest(turtle: Turtle, link: Link) = false
  def isSource(turtle: Turtle, link: Link) = false

  override def getSource(link: Link): Turtle = null
  override def getDest(link: Link): Turtle = null

}