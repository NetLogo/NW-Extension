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

  val rng = new scala.util.Random(world.mainRNG)

  private var monitoredTurtleSet: MonitoredAgentSet[Turtle] = {
    val result = initialTurtleSet match {
      case tas: TreeAgentSet  => new MonitoredTurtleTreeAgentSet(tas, this)
      case aas: ArrayAgentSet => new MonitoredTurtleArrayAgentSet(aas, this)
    }
    initialTurtleSet = null
    result
  }

  private var monitoredLinkSet: MonitoredAgentSet[Link] = {
    val result = initialLinkSet match {
      case tas: TreeAgentSet  => new MonitoredLinkTreeAgentSet(tas, this)
      case aas: ArrayAgentSet => new MonitoredLinkArrayAgentSet(aas, this)
    }
    initialLinkSet = null
    result
  }

  def turtleSet = monitoredTurtleSet.agentSet
  def linkSet = monitoredLinkSet.agentSet

  def verify(): Unit = {
    Seq(monitoredTurtleSet, monitoredLinkSet).collect {
      case maas: MonitoredArrayAgentSet[_] => maas.verify()
    }
  }

  def invalidate(): Unit = {
    monitoredTurtleSet.reset()
    monitoredLinkSet.reset()
    directedJungGraph = None
    undirectedJungGraph = None
  }

  def asJungGraph: jung.Graph = if (isDirected) asDirectedJungGraph else asUndirectedJungGraph
  private var directedJungGraph: Option[jung.DirectedGraph] = None
  def asDirectedJungGraph: jung.DirectedGraph = {
    verify()
    directedJungGraph
      .getOrElse {
        val g = new jung.DirectedGraph(this)
        directedJungGraph = Some(g)
        g
      }
  }
  private var undirectedJungGraph: Option[jung.UndirectedGraph] = None
  def asUndirectedJungGraph: jung.UndirectedGraph = {
    verify()
    undirectedJungGraph
      .getOrElse {
        val g = new jung.UndirectedGraph(this)
        undirectedJungGraph = Some(g)
        g
      }
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

  def isValidLink(link: Link) =
    monitoredLinkSet.isValid(link) &&
      isValidTurtle(link.end1) &&
      isValidTurtle(link.end2)

  def validLink(link: Link): Option[Link] =
    if (isValidLink(link)) Some(link) else None

  def turtleCount: Int = turtleSet.count
  def linkCount: Int = linkSet.count

  def links: Iterable[Link] = linkSet.asShufflerable[Link](world.mainRNG)
  def turtles: Iterable[Turtle] = turtleSet.asShufflerable[Turtle](world.mainRNG)

  // LinkManager.findLink* methods require "breed" agentsets and, as such,
  // does not play well with linkSet in the case it's an ArrayAgentSet.
  // This is why we pass world.links to the methods and do the filtering
  // ourselves afterwards.
  // NP 2013-07-11.
  def edges(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean): Iterable[Link] =
    rng.shuffle(
      linkManager
        .findLinksWith(turtle, world.links)
        .asIterable[Link]
        .collect {
          case l if includeUn && !l.isDirectedLink && isValidLink(l)                     => l
          case l if includeOut && l.isDirectedLink && l.end1 == turtle && isValidLink(l) => l
          case l if includeIn && l.isDirectedLink && l.end2 == turtle && isValidLink(l)  => l
        })

  def neighbors(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean): Iterable[Turtle] = {
    def ok(t: Turtle) = t != turtle && isValidTurtle(t)
    edges(turtle, includeUn, includeIn, includeOut).collect {
      case l if ok(l.end1) => l.end1
      case l if ok(l.end2) => l.end2
    }
  }

  def undirectedEdges(turtle: Turtle): Iterable[Link] = edges(turtle, true, false, false)
  def undirectedNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, true, false, false)

  def directedInEdges(turtle: Turtle): Iterable[Link] = edges(turtle, false, true, false)
  def inNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, false, true, false)

  def directedOutEdges(turtle: Turtle): Iterable[Link] = edges(turtle, false, false, true)
  def outNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, false, false, true)

  def allEdges(turtle: Turtle): Iterable[Link] = edges(turtle, true, true, true)
  def allNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, true, true, true)

  // Jung, weirdly, sometimes uses in/outedges with undirected graphs, actually expecting all edges
  def inEdges(turtle: Turtle): Iterable[Link] =
    if (isDirected) directedInEdges(turtle) else allEdges(turtle)
  def outEdges(turtle: Turtle): Iterable[Link] =
    if (isDirected) directedOutEdges(turtle) else allEdges(turtle)

  override def toString = turtleSet.toLogoList + "\n" + linkSet.toLogoList
}
