// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import org.nlogo.agent._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.algorithms.{BreadthFirstSearch, PathFinder}
import org.nlogo.api.MersenneTwisterFast
import scala.collection.{GenIterable, mutable}
import org.nlogo.api.ExtensionException
import scala.Some
import org.nlogo.extensions.nw.util.CacheManager
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class GraphContext(
  val world: World,
  val turtleSet: AgentSet,
  val linkSet: AgentSet)
    extends Graph[Turtle, Link]
    with algorithms.CentralityMeasurer
    with algorithms.ClusteringMetrics[Turtle, Link] {

  implicit val implicitWorld = world

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
    .filter((l => turtles.contains(l.end1) && turtles.contains(l.end2)))
    .toSet

  val (undirLinks, inLinks, outLinks) = {
    val in = mutable.Map.empty[Turtle, List[Link]] withDefaultValue List.empty[Link]
    val out = mutable.Map.empty[Turtle, List[Link]] withDefaultValue List.empty[Link]
    val undir = mutable.Map.empty[Turtle, List[Link]] withDefaultValue List.empty[Link]
    links foreach { link =>

      if (link.isDirectedLink) {
        out(link.end1) = link +: out(link.end1)
        in(link.end2) = link +: in(link.end2)
      } else {
        undir(link.end1) = link +: undir(link.end1)
        undir(link.end2) = link +: undir(link.end2)
      }
    }
    (undir.toMap withDefaultValue List.empty[Link],
     in.toMap withDefaultValue List.empty[Link],
     out.toMap withDefaultValue List.empty[Link])
  }

  def verify(w: World): GraphContext = {
    if (w != world) {
      new GraphContext(w, w.turtles(), w.links())
    } else if (turtleMonitor.hasChanged || linkMonitor.hasChanged) {
      // When a resize occurs, breed sets are all set to new objects, so we
      // need to make sure we're working with the latest object.
      new GraphContext(w,
        if (turtleSet.isInstanceOf[TreeAgentSet]) Option(w.getBreed(turtleSet.printName)).getOrElse(w.turtles()) else turtleSet,
        if (linkSet.isInstanceOf[TreeAgentSet]) Option(w.getLinkBreed(linkSet.printName)).getOrElse(w.links()) else linkSet)
    } else {
      this
    }
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

  // linkSet.isDirected fails for empty and mixed directed networks. The only
  // reliable way is to actually see if there are any directed links.
  // -- BCH 5/13/2014
  lazy val isDirected = !outLinks.isEmpty

  def turtleCount: Int = turtles.size
  def linkCount: Int = links.size

  override def ends(link: Link): (Turtle, Turtle) = (link.end1, link.end2)

  override def inEdges(turtle: Turtle): Seq[Link] = inLinks(turtle) ::: undirLinks(turtle)

  override def outEdges(turtle: Turtle): Seq[Link] = outLinks(turtle) ::: undirLinks(turtle)

  override def allEdges(turtle: Turtle): Seq[Link] = inLinks(turtle) ::: outLinks(turtle) ::: undirLinks(turtle)

  override def toString = turtleSet.toLogoList + "\n" + linkSet.toLogoList

  val pathFinder = new PathFinder[Turtle, Link](this, world, weightFunction _)

  lazy val components: Traversable[Set[Turtle]] = {
    val foundBy = mutable.Map[Turtle, Turtle]()
    turtles.groupBy { t =>
      foundBy.getOrElseUpdate(t, {
        BreadthFirstSearch(this, t)
          .map(_.head)
          .foreach(found => foundBy(found) = t)
        t
      })
    }.values
  }

  def monitoredTreeAgentSets =
    Seq(turtleMonitor, linkMonitor).collect {
      case mtas: MonitoredTreeAgentSet[_] => mtas
    }

  def unsubscribe(): Unit = monitoredTreeAgentSets.foreach(_.unsubscribe())
}
