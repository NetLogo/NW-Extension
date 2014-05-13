// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import org.nlogo.agent._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.util.MersenneTwisterFast
import scala.collection.{GenIterable, mutable}
import org.nlogo.api.ExtensionException
import scala.Some

class GraphContext(
  val world: World,
  val turtleSet: AgentSet,
  val linkSet: AgentSet) extends algorithms.Graph {

  val rng = new scala.util.Random(world.mainRNG)
  val turtleMonitor = turtleSet match {
    case tas: TreeAgentSet  => new MonitoredTurtleTreeAgentSet(tas)
    case aas: ArrayAgentSet => new MonitoredTurtleArrayAgentSet(aas)
  }

  val linkMonitor = linkSet match {
    case tas: TreeAgentSet  => new MonitoredLinkTreeAgentSet(tas)
    case aas: ArrayAgentSet => new MonitoredLinkArrayAgentSet(aas)
  }

  val turtles: Set[Turtle] = turtleSet.asIterable[Turtle].toSet
  val links: Set[Link] = linkSet.asIterable[Link].toSet
  private val inLinks: mutable.Map[Turtle, mutable.ArrayBuffer[Link]] = mutable.Map()
  private val outLinks: mutable.Map[Turtle, mutable.ArrayBuffer[Link]] = mutable.Map()
  private val undirLinks: mutable.Map[Turtle, mutable.ArrayBuffer[Link]] = mutable.Map()

  for (turtle: Turtle <- turtles) {
    inLinks(turtle) = mutable.ArrayBuffer(): mutable.ArrayBuffer[Link]
    outLinks(turtle) = mutable.ArrayBuffer(): mutable.ArrayBuffer[Link]
    undirLinks(turtle) = mutable.ArrayBuffer(): mutable.ArrayBuffer[Link]
  }

  for (link: Link <- links) {
    if (turtles.contains(link.end1) && turtles.contains(link.end2)) {
      if (link.isDirectedLink) {
        outLinks(link.end1) += link
        inLinks(link.end2) += link
      } else {
        undirLinks(link.end1) += link
        undirLinks(link.end2) += link
      }
    }
  }

  def verify(w: World): GraphContext =
    if (w != world) {
      clearAllCaches() // Clear watchers in particular
      new GraphContext(w, w.turtles(), w.links())
    } else if (w != world || turtleMonitor.hasChanged || linkMonitor.hasChanged) {
      clearAllCaches() // Clear watchers in particular
      new GraphContext(w, turtleSet, linkSet)
    } else {
      this
    }

  def asJungGraph: jung.Graph = if (isDirected) asDirectedJungGraph else asUndirectedJungGraph
  private var directedJungGraph: Option[jung.DirectedGraph] = None
  def asDirectedJungGraph: jung.DirectedGraph = {
    directedJungGraph
      .getOrElse {
        val g = new jung.DirectedGraph(this)
        directedJungGraph = Some(g)
        g
      }
  }
  private var undirectedJungGraph: Option[jung.UndirectedGraph] = None
  def asUndirectedJungGraph: jung.UndirectedGraph = {
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
  // This is currently broken for mixed directedness contexts
  def isDirected = linkSet.isDirected

  def turtleCount: Int = turtles.size
  def linkCount: Int = links.size

  def edges(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean): Iterable[Link] =
    rng.shuffle(
      (if (includeUn) undirLinks.getOrElse(turtle, Seq()) else Seq()) ++
      (if (includeIn) inLinks.getOrElse(turtle, Seq()) else Seq()) ++
      (if (includeOut) outLinks.getOrElse(turtle, Seq()) else Seq())
    )



  def neighbors(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean): Iterable[Turtle] = {
    edges(turtle, includeUn, includeIn, includeOut) map { l: Link =>
      if (l.end1 == turtle) l.end2 else l.end1
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

  override def toString = turtleSet.toLogoList + "\n" + linkSet.toLogoList


  // Initializing with in-degree works well with directed graphs, knocking out obviously non-strongly reachable nodes
  // immediately. Initializing with all ones can make convergence take much longer. -- BCH 5/12/2014
  private lazy val inDegrees = turtles.foldLeft(Map.empty[Turtle, Double])(
    (m, t) => m + (t -> neighbors(t, includeUn = true, includeIn = true, includeOut = false).size.toDouble))

  lazy val eigenvectorCentrality = Iterator.iterate(inDegrees)((last) => {
    val result = last map {
      // Leaving the last score allows us to handle networks for which power iteration normally fails, e.g. 0--1--2
      // Gephi does this -- BCH 5/12/2014
      case (turtle: Turtle, lastScore: Double) =>
        turtle -> (lastScore + (neighbors(turtle, includeUn = true, includeIn = true, includeOut = false) map last).sum)
    }
    // This is how gephi normalizes -- BCH 5/12/2014
    val normalizer = result.values.max
    if (normalizer > 0) {
      result map {
        case (turtle: Turtle, score: Double) => turtle -> score / normalizer
      }
    } else {
      // Everything is disconnected... just give everyone 1s -- BCH 5/12/2014
      result map {
        case (turtle: Turtle, score: Double) => turtle -> 1.0
      }
    }
  }).drop(100).next()

}
