// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import org.nlogo.agent._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.api.MersenneTwisterFast
import scala.collection.{GenIterable, mutable}
import org.nlogo.api.ExtensionException
import scala.Some
import org.nlogo.extensions.nw.util.CacheManager
import scala.collection.mutable.ArrayBuffer

class GraphContext(
  val world: World,
  val turtleSet: AgentSet,
  val linkSet: AgentSet)
    extends algorithms.PathFinder
    with algorithms.CentralityMeasurer
    with algorithms.ClusteringMetrics {

  implicit val implicitWorld = world

  val rng = new scala.util.Random(world.mainRNG)
  val turtleMonitor = turtleSet match {
    case tas: TreeAgentSet  => new MonitoredTurtleTreeAgentSet(tas, world)
    case aas: ArrayAgentSet => new MonitoredTurtleArrayAgentSet(aas)
  }

  val linkMonitor = linkSet match {
    case tas: TreeAgentSet  => new MonitoredLinkTreeAgentSet(tas, world)
    case aas: ArrayAgentSet => new MonitoredLinkArrayAgentSet(aas)
  }

  val turtles: Set[Turtle] = turtleSet.asIterable[Turtle].toSet

  val links: Set[Link] = linkSet.asIterable[Link]
    .filter(link => turtles.contains(link.end1) && turtles.contains(link.end2))
    .toSet

  private val inLinks: mutable.Map[Turtle, mutable.ArrayBuffer[Link]] = mutable.Map()
  private val outLinks: mutable.Map[Turtle, mutable.ArrayBuffer[Link]] = mutable.Map()
  private val undirLinks: mutable.Map[Turtle, mutable.ArrayBuffer[Link]] = mutable.Map()

  for (turtle: Turtle <- turtles) {
    inLinks(turtle) = mutable.ArrayBuffer(): mutable.ArrayBuffer[Link]
    outLinks(turtle) = mutable.ArrayBuffer(): mutable.ArrayBuffer[Link]
    undirLinks(turtle) = mutable.ArrayBuffer(): mutable.ArrayBuffer[Link]
  }

  for (link: Link <- links) {
    if (link.isDirectedLink) {
      outLinks(link.end1) += link
      inLinks(link.end2) += link
    } else {
      undirLinks(link.end1) += link
      undirLinks(link.end2) += link
    }
  }

  def verify(w: World): GraphContext =
    if (w != world) {
      new GraphContext(w, w.turtles(), w.links())
    } else if (w != world || turtleMonitor.hasChanged || linkMonitor.hasChanged) {
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

  // I tried caching this, but it only made SingleSourceWeighted benchmark ~5% faster; not significant
  // enough to be worth the memory. -- BCH 5/14/2014
  def weightFunction(variable: String): (Link => Double) = {
    (link: Link) =>
      try {
        link.world.program.linksOwn.indexOf(variable) match {
          case -1 => link.getLinkBreedVariable(variable).asInstanceOf[Double]
          case i  => link.getLinkVariable(i).asInstanceOf[Double]
        }
      } catch {
        case e: ClassCastException => throw new ExtensionException("Weights must be numbers.", e)
        case e: Exception => throw new ExtensionException(e)
      }
  }

  /*
  linkSet.isDirected fails for empty and mixed directed networks. The only reliable way I've found to detect
  directedness is by literally checking each link. If any are directed, we consider the whole thing to be directed.
  -- BCH 5/13/2014
 */
  lazy val isDirected = links exists { _.isDirectedLink }

  def turtleCount: Int = turtles.size
  def linkCount: Int = links.size

  def edges(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean, shuffle: Boolean = true): Iterable[Link] = {
    // Using mutable stuff here actually made a significant performance different (>10%), but I do
    // feel bad about it -- BCH 5/14/2014
    val result = ArrayBuffer.empty[Link]
    if (includeUn) result ++= undirLinks.getOrElse(turtle, ArrayBuffer.empty)
    if (includeIn) result ++= inLinks.getOrElse(turtle, ArrayBuffer.empty)
    if (includeOut) result ++= outLinks.getOrElse(turtle, ArrayBuffer.empty)
    if (shuffle) rng.shuffle(result) else result
  }

  def neighbors(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean, shuffle: Boolean = true): Iterable[Turtle] = {
    edges(turtle, includeUn, includeIn, includeOut, shuffle) map { l: Link =>
      if (l.end1 == turtle) l.end2 else l.end1
    }
  }

  def undirectedEdges(turtle: Turtle): Iterable[Link] = edges(turtle, true, false, false)
  def undirectedNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, true, false, false)

  def inEdges(turtle: Turtle): Iterable[Link] = edges(turtle, includeUn = true, includeIn = true, includeOut = false)
  def inNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, includeUn = true, includeIn = true, includeOut = false)

  def directedInEdges(turtle: Turtle): Iterable[Link] = edges(turtle, false, true, false)
  def directedInNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, false, true, false)

  def outEdges(turtle: Turtle): Iterable[Link] = edges(turtle, includeUn = true, includeIn = false, includeOut = true)
  def outNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, includeUn = true, includeIn = false, includeOut = true)

  def directedOutEdges(turtle: Turtle): Iterable[Link] = edges(turtle, false, false, true)
  def directedOutNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, false, false, true)

  def allEdges(turtle: Turtle): Iterable[Link] = edges(turtle, true, true, true)
  def allNeighbors(turtle: Turtle): Iterable[Turtle] = neighbors(turtle, true, true, true)

  override def toString = turtleSet.toLogoList + "\n" + linkSet.toLogoList

  def monitoredTreeAgentSets =
    Seq(turtleMonitor, linkMonitor).collect {
      case mtas: MonitoredTreeAgentSet[_] => mtas
    }

  def unsubscribe(): Unit = monitoredTreeAgentSets.foreach(_.unsubscribe())
}
