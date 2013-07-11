// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import org.nlogo.agent.AgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.TreeAgentSet
import org.nlogo.agent.Turtle
import org.nlogo.agent.World
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.agent.ArrayAgentSet
import org.nlogo.util.MersenneTwisterFast

class GraphContext(
  val world: World,
  private var initialTurtleSet: AgentSet,
  private var initialLinkSet: AgentSet) {

  val rng: MersenneTwisterFast = world.mainRNG

  private var monitoredTurtleSet: MonitoredAgentSet[Turtle] = {
    val result = initialTurtleSet match {
      case tas: TreeAgentSet  => new MonitoredTurtleTreeAgentSet(tas, invalidate)
      case aas: ArrayAgentSet => new MonitoredTurtleArrayAgentSet(aas, () => Unit)
    }
    initialTurtleSet = null
    result
  }

  private var monitoredLinkSet: MonitoredAgentSet[Link] = {
    val result = initialLinkSet match {
      case tas: TreeAgentSet  => new MonitoredLinkTreeAgentSet(tas, invalidate)
      case aas: ArrayAgentSet => new MonitoredLinkArrayAgentSet(aas, () => Unit)
    }
    initialLinkSet = null
    result
  }

  def turtleSet = monitoredTurtleSet.agentSet
  def linkSet = monitoredLinkSet.agentSet

  def invalidate(): Unit = {
    monitoredTurtleSet.invalidate()
    monitoredLinkSet.invalidate()
    directedJungGraph = None
    undirectedJungGraph = None
  }

  def asJungGraph: jung.Graph = if (isDirected) asDirectedJungGraph else asUndirectedJungGraph
  private var directedJungGraph: Option[jung.DirectedGraph] = None
  def asDirectedJungGraph: jung.DirectedGraph =
    directedJungGraph
      .getOrElse {
        val g = new jung.DirectedGraph(this)
        directedJungGraph = Some(g)
        g
      }
  private var undirectedJungGraph: Option[jung.UndirectedGraph] = None
  def asUndirectedJungGraph: jung.UndirectedGraph =
    undirectedJungGraph
      .getOrElse {
        val g = new jung.UndirectedGraph(this)
        undirectedJungGraph = Some(g)
        g
      }

  def asJGraphTGraph: jgrapht.Graph = if (isDirected) asDirectedJGraphTGraph else asUndirectedJGraphTGraph
  lazy val asDirectedJGraphTGraph = new jgrapht.DirectedGraph(this)
  lazy val asUndirectedJGraphTGraph = new jgrapht.UndirectedGraph(this)

  /* Until an actual link has been created, the directedness of the links agentset
   * is not defined: i.e., both  linkSet.isDirected and linkSet.isUndirected will 
   * return false. Here, we just check for .isDirected because if no links have been 
   * created, treating the graph as undirected will do no harm. NP 2013-05-15
   */
  def isDirected = linkSet.isDirected

  val linkManager = world.linkManager

  def isValidTurtle(turtle: Turtle) = monitoredTurtleSet.isValid(turtle)
  def validTurtle(turtle: Turtle): Option[Turtle] =
    if (isValidTurtle(turtle)) Some(turtle) else None

  def isValidLink(link: Link) = monitoredLinkSet.isValid(link) &&
    isValidTurtle(link.end1) && isValidTurtle(link.end2)
  def validLink(link: Link): Option[Link] =
    if (isValidLink(link)) Some(link) else None

  def turtleCount: Int = turtleSet.count
  def linkCount: Int = linkSet.count

  def links: Iterable[Link] = linkSet.asIterable[Link](rng)
  def turtles: Iterable[Turtle] = turtleSet.asIterable[Turtle](rng)

  // LinkManager.findLink* methods require "breed" agentsets and, as such,
  // does not play well with linkSet in the case it's an ArrayAgentSet.
  // This is why we pass world.links to the methods and do the filtering
  // ourselves afterwards. Making MonitoredArrayAgentSet.isValid more efficient
  // (it's currently O(n)) would go a long way towards making this sensible.
  // NP 2013-07-11.
  def allEdges(turtle: Turtle): Iterable[Link] =
    linkManager.findLinksWith(turtle, world.links).asIterable[Link](rng).filter(isValidLink)
  def allNeighbors(turtle: Turtle): Iterable[Turtle] =
    linkManager.findLinkedWith(turtle, world.links).asIterable[Turtle](rng).filter(isValidTurtle)

  def directedInEdges(turtle: Turtle): Iterable[Link] =
    linkManager.findLinksTo(turtle, world.links).asIterable[Link](rng).filter(isValidLink)
  def inNeighbors(turtle: Turtle): Iterable[Turtle] =
    linkManager.findLinkedTo(turtle, world.links).asIterable[Turtle](rng).filter(isValidTurtle)

  def directedOutEdges(turtle: Turtle): Iterable[Link] =
    linkManager.findLinksFrom(turtle, world.links).asIterable[Link](rng).filter(isValidLink)
  def outNeighbors(turtle: Turtle): Iterable[Turtle] =
    linkManager.findLinkedFrom(turtle, world.links).asIterable[Turtle](rng).filter(isValidTurtle)

  // Jung, weirdly, sometimes uses in/outedges with undirected graphs, actually expecting all edges
  def inEdges(turtle: Turtle): Iterable[Link] =
    if (isDirected) directedInEdges(turtle) else allEdges(turtle)
  def outEdges(turtle: Turtle): Iterable[Link] =
    if (isDirected) directedOutEdges(turtle) else allEdges(turtle)

  override def toString = turtleSet.toLogoList + "\n" + linkSet.toLogoList
}
