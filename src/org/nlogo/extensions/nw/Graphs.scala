package org.nlogo.extensions.nw

import scala.Option.option2Iterable
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

import org.nlogo.agent.Agent
import org.nlogo.agent.AgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.extensions.nw.GraphUtil.EnrichAgentSet

object GraphUtil {
  implicit def EnrichAgentSet(agentSet: AgentSet) = new RichAgentSet(agentSet)
  class RichAgentSet(agentSet: AgentSet) {
    def toIterable[T <: Agent] = agentSet.agents.asScala.map(_.asInstanceOf[T])
  }
}

trait NetLogoGraph {
  val isStatic: Boolean
  val isValidLink: Link => Boolean
  val isValidTurtle: Turtle => Boolean
  def validTurtle(t: Turtle) = Option(t).filter(isValidTurtle)
  def validLink(l: Link) = Option(l).filter(isValidLink)
  // TODO: if I figure out how, get rid of validTurtle / validLink in favor of:
  // def valid[A](obj: A)(implicit isValid: A => Boolean) = Option(obj).filter(isValid)

  def asJungGraph = if (isDirected) asDirectedJungGraph else asUndirectedJungGraph
  lazy val asDirectedJungGraph = new nl.jung.DirectedGraph(this)
  lazy val asUndirectedJungGraph = new nl.jung.UndirectedGraph(this)

//  def asJGraphTGraph = if (isDirected) asDirectedJGraphTGraph else asUndirectedJGraphTGraph
  def asJGraphTGraph = asUndirectedJGraphTGraph
//  lazy val asDirectedJGraphTGraph = new nl.jgrapht.DirectedGraph(this)
  lazy val asUndirectedJGraphTGraph = new nl.jgrapht.UndirectedGraph(this)

  protected val linkSet: AgentSet
  val world = linkSet.world
  val rng = world.mainRNG

  lazy val isDirected = links.forall(_.isDirectedLink)

  def links: Iterable[Link]
  def turtles: Iterable[Turtle]

  def allEdges(turtle: Turtle): Iterable[Link]

  def directedInEdges(turtle: Turtle): Iterable[Link]
  def directedOutEdges(turtle: Turtle): Iterable[Link]

  // Jung, weirdly, sometimes uses in/outedges with undirected graphs, actually expecting all edges
  def inEdges(turtle: Turtle) =
    if (isDirected) directedInEdges(turtle) else allEdges(turtle)
  def outEdges(turtle: Turtle) =
    if (isDirected) directedOutEdges(turtle) else allEdges(turtle)

}

class StaticNetLogoGraph(
  protected val linkSet: AgentSet,
  protected val turtleSet: AgentSet)
  extends NetLogoGraph {

  val isStatic = true

  override val isValidLink = _links.contains(_: Link)
  override val isValidTurtle = _turtles.contains(_: Turtle)

  private val _turtles = Set() ++ turtleSet.toIterable[Turtle]
  private val _links = Set() ++ linkSet.toIterable[Link]
    .filter { l => _turtles.contains(l.end1) && _turtles.contains(l.end2) }

  def turtles = _turtles: Iterable[Turtle]
  def links = _links: Iterable[Link]

  private type LinkMap = Map[Turtle, Iterable[Link]]

  private lazy val allEdgesMap: LinkMap =
    turtles.map(t => t -> _links.filter(l => l.end1 == t || l.end2 == t)).toMap
  def allEdges(turtle: Turtle) = allEdgesMap.get(turtle).flatten

  private lazy val inEdgesMap: LinkMap = links.groupBy(_.end2)
  override def directedInEdges(turtle: Turtle) = inEdgesMap.get(turtle).flatten

  private lazy val outEdgesMap: LinkMap = links.groupBy(_.end1)
  override def directedOutEdges(turtle: Turtle) = outEdgesMap.get(turtle).flatten
}