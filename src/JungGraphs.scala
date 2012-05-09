package org.nlogo.extensions.nw

import java.util.Collection

import scala.collection.JavaConverters.asJavaCollectionConverter

import org.nlogo.agent.AgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath
import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.graph.util.Pair
import edu.uci.ics.jung.graph.DirectedGraph
import edu.uci.ics.jung.graph.UndirectedGraph
import edu.uci.ics.jung.graph.AbstractGraph
import edu.uci.ics.jung.graph.AbstractTypedGraph
import edu.uci.ics.jung.graph.Graph

trait JungGraph
  extends Graph[Turtle, Link] {
  self: NetLogoGraph =>

  lazy val dijkstraShortestPath = new DijkstraShortestPath(this, isStatic)

  override def getInEdges(turtle: Turtle): Collection[Link] =
    validTurtle(turtle).map(inEdges(_).asJavaCollection).orNull
  override def getPredecessors(turtle: Turtle): Collection[Turtle] =
    validTurtle(turtle).map(inEdges(_).map(_.end1).asJavaCollection).orNull

  override def getOutEdges(turtle: Turtle): Collection[Link] =
    validTurtle(turtle).map(outEdges(_).asJavaCollection).orNull
  override def getSuccessors(turtle: Turtle): Collection[Turtle] =
    validTurtle(turtle).map(outEdges(_).map(_.end2).asJavaCollection).orNull

  override def getIncidentEdges(turtle: Turtle): Collection[Link] =
    validTurtle(turtle).map(edges(_).asJavaCollection).orNull

  override def getSource(link: Link): Turtle =
    validLink(link).filter(_.isDirectedLink).map(_.end1).orNull

  override def getDest(link: Link): Turtle =
    validLink(link).filter(_.isDirectedLink).map(_.end2).orNull

  override def getEdgeCount(): Int = links.size

  override def getNeighbors(turtle: Turtle): Collection[Turtle] =
    validTurtle(turtle).map { t =>
      (outEdges(t).map(_.end2) ++ inEdges(t).map(_.end1)).asJavaCollection
    }.orNull

  override def getVertexCount(): Int = turtles.size
  override def getVertices(): Collection[Turtle] = turtles.asJavaCollection
  override def getEdges(): Collection[Link] = links.asJavaCollection
  override def containsEdge(link: Link): Boolean = validLink(link).isDefined
  override def containsVertex(turtle: Turtle): Boolean = validTurtle(turtle).isDefined

  def getEndpoints(link: Link): Pair[Turtle] =
    new Pair(link.end1, link.end2) // Note: contract says nothing about edge being in graph

  def isDest(turtle: Turtle, link: Link): Boolean =
    validLink(link).filter(_.end2 == turtle).isDefined
  def isSource(turtle: Turtle, link: Link): Boolean =
    validLink(link).filter(_.end1 == turtle).isDefined

  // TODO: in a live graph, maybe they could be useful for generators
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

trait UntypedAbstractJungGraph
  extends AbstractGraph[Turtle, Link] {
  self: NetLogoGraph =>
  override def getEdgeType(link: Link): EdgeType =
    if (link.isDirectedLink) EdgeType.DIRECTED else EdgeType.UNDIRECTED
  private def edges(edgeType: EdgeType) = links.filter(getEdgeType(_) == edgeType)
  override def getEdgeCount(edgeType: EdgeType) = edges(edgeType).size
  override def getEdges(edgeType: EdgeType) = edges(edgeType).asJavaCollection
  override def getDefaultEdgeType(): EdgeType = EdgeType.UNDIRECTED
}

trait DirectedJungGraph
  extends JungGraph
  with DirectedGraph[Turtle, Link] {
  self: NetLogoGraph =>
}

trait UndirectedJungGraph
  extends JungGraph
  with UndirectedGraph[Turtle, Link] {
  self: NetLogoGraph =>
}

class LiveJungGraph(val linkSet: AgentSet)
  extends UntypedAbstractJungGraph
  with JungGraph
  with LiveNetLogoGraph

class DirectedLiveJungGraph(val linkSet: AgentSet)
  extends AbstractTypedGraph[Turtle, Link](EdgeType.DIRECTED)
  with DirectedJungGraph {
  self: LiveNetLogoGraph =>
  if (!linkSet.isDirected)
    throw new ExtensionException("link set must be directed")
}

class UndirectedLiveJungGraph(val linkSet: AgentSet)
  extends AbstractTypedGraph[Turtle, Link](EdgeType.UNDIRECTED)
  with UndirectedJungGraph {
  self: LiveNetLogoGraph =>
  if (!linkSet.isUndirected)
    throw new ExtensionException("link set must be undirected")
}

class StaticJungGraph(
  val linkSet: AgentSet,
  val turtleSet: AgentSet)
  extends UntypedAbstractJungGraph
  with JungGraph
  with StaticNetLogoGraph

class DirectedStaticJungGraph(
  val linkSet: AgentSet,
  val turtleSet: AgentSet)
  extends AbstractTypedGraph[Turtle, Link](EdgeType.DIRECTED)
  with DirectedJungGraph
  with StaticNetLogoGraph {
  // TODO: require directed graph
}

class UndirectedStaticJungGraph(
  val linkSet: AgentSet,
  val turtleSet: AgentSet)
  extends AbstractTypedGraph[Turtle, Link](EdgeType.UNDIRECTED)
  with UndirectedJungGraph
  with StaticNetLogoGraph {
  // TODO: require undirected graph
}