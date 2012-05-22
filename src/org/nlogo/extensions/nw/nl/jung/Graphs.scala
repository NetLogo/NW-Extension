package org.nlogo.extensions.nw.nl.jung

import java.util.Collection

import scala.collection.JavaConverters.asJavaCollectionConverter

import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import org.nlogo.extensions.nw.NetLogoGraph
import org.nlogo.extensions.nw.nl

import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.graph.util.Pair
import edu.uci.ics.jung

trait Graph
  extends jung.graph.AbstractGraph[Turtle, Link]
  with nl.jung.Algorithms {

  val nlg: NetLogoGraph

  override def getIncidentEdges(turtle: Turtle): Collection[Link] =
    nlg.validTurtle(turtle).map(nlg.allEdges(_).asJavaCollection).orNull

  override def getEdgeCount(): Int = nlg.links.size

  override def getNeighbors(turtle: Turtle): Collection[Turtle] =
    nlg.validTurtle(turtle).map { t =>
      (nlg.outEdges(t).map(_.end2) ++ nlg.inEdges(t).map(_.end1)).asJavaCollection
    }.orNull

  override def getVertexCount(): Int = nlg.turtles.size
  override def getVertices(): Collection[Turtle] = nlg.turtles.asJavaCollection
  override def getEdges(): Collection[Link] = nlg.links.asJavaCollection
  override def containsEdge(link: Link): Boolean = nlg.validLink(link).isDefined
  override def containsVertex(turtle: Turtle): Boolean = nlg.validTurtle(turtle).isDefined

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

}

class DirectedGraph(
  override val nlg: NetLogoGraph)
  extends jung.graph.AbstractTypedGraph[Turtle, Link](EdgeType.DIRECTED)
  with nl.jung.Graph
  with nl.jung.DirectedAlgorithms
  with jung.graph.DirectedGraph[Turtle, Link] {

  if (!nlg.isDirected)
    throw new ExtensionException("link set must be directed")

  override def getInEdges(turtle: Turtle): Collection[Link] =
    nlg.validTurtle(turtle).map(nlg.inEdges(_).asJavaCollection).orNull
  override def getPredecessors(turtle: Turtle): Collection[Turtle] =
    nlg.validTurtle(turtle).map(nlg.inEdges(_).map(_.end1).asJavaCollection).orNull

  override def getOutEdges(turtle: Turtle): Collection[Link] =
    nlg.validTurtle(turtle).map(nlg.outEdges(_).asJavaCollection).orNull
  override def getSuccessors(turtle: Turtle): Collection[Turtle] =
    nlg.validTurtle(turtle).map(nlg.outEdges(_).map(_.end2).asJavaCollection).orNull

  def isDest(turtle: Turtle, link: Link): Boolean =
    nlg.validLink(link).filter(_.end2 == turtle).isDefined
  def isSource(turtle: Turtle, link: Link): Boolean =
    nlg.validLink(link).filter(_.end1 == turtle).isDefined

  override def getSource(link: Link): Turtle =
    nlg.validLink(link).map(_.end1).orNull

  override def getDest(link: Link): Turtle =
    nlg.validLink(link).map(_.end2).orNull

}

class UndirectedGraph(
  override val nlg: NetLogoGraph)
  extends jung.graph.AbstractTypedGraph[Turtle, Link](EdgeType.UNDIRECTED)
  with nl.jung.Graph
  with nl.jung.UndirectedAlgorithms
  with jung.graph.UndirectedGraph[Turtle, Link] {

  override def getInEdges(turtle: Turtle) = getIncidentEdges(turtle)
  override def getPredecessors(turtle: Turtle) = getNeighbors(turtle: Turtle)

  override def getOutEdges(turtle: Turtle) = getIncidentEdges(turtle)
  override def getSuccessors(turtle: Turtle) = getNeighbors(turtle: Turtle)

  def isDest(turtle: Turtle, link: Link) = false
  def isSource(turtle: Turtle, link: Link) = false

  override def getSource(link: Link): Turtle = null
  override def getDest(link: Link): Turtle = null

}