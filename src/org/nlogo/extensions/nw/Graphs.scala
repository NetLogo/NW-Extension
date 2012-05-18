package org.nlogo.extensions.nw

import org.nlogo.agent.Agent
import org.nlogo.agent.AgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import org.nlogo.api.ExtensionObject
import org.nlogo.extensions.nw.GraphUtil.EnrichAgentSet
import scala.Option.option2Iterable
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

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
  lazy val asDirectedJungGraph = new DirectedJungGraph(this)
  lazy val asUndirectedJungGraph = new UndirectedJungGraph(this)

  protected val linkSet: AgentSet
  val world = linkSet.world
  
  /* TODO: the directednes of world.links is set to directed 
   * only if unbreeded links are directed. So if we have breeds,
   * even if all breeds are directed, the type of "links" will
   * be undirected. This is a bit counterintuitive - find a fix...  
   */
  
  val isDirected = linkSet.isDirected  
  
  def links: Iterable[Link]
  def turtles: Iterable[Turtle]

  def edges(turtle: Turtle): Iterable[Link]

  def directedInEdges(turtle: Turtle): Iterable[Link]
  def directedOutEdges(turtle: Turtle): Iterable[Link]

  def inEdges(turtle: Turtle) =
    if (isDirected) directedInEdges(turtle) else edges(turtle)
  def outEdges(turtle: Turtle) =
    if (isDirected) directedOutEdges(turtle) else edges(turtle)

}



class StaticNetLogoGraph(
  protected val linkSet: AgentSet,
  protected val turtleSet: AgentSet)
  extends NetLogoGraph {

  val isStatic = true

  override val isValidLink = linkVector.contains(_: Link)
  override val isValidTurtle = turtleVector.contains(_: Turtle)

  private val turtleVector = Vector() ++ turtleSet.toIterable[Turtle]
  private val linkVector = Vector() ++ linkSet.toIterable[Link]
    .filter { l => turtleVector.contains(l.end1) && turtleVector.contains(l.end2) }

  def turtles = turtleVector: Iterable[Turtle]
  def links = linkVector: Iterable[Link]

  def edges(turtle: Turtle) = linkVector.filter(l => l.end1 == turtle || l.end2 == turtle)

  private lazy val inEdgesMap: Map[Turtle, Iterable[Link]] = links.groupBy(_.end2)
  override def directedInEdges(turtle: Turtle) = inEdgesMap.get(turtle).flatten

  private lazy val outEdgesMap: Map[Turtle, Iterable[Link]] = links.groupBy(_.end1)
  override def directedOutEdges(turtle: Turtle) = outEdgesMap.get(turtle).flatten
}