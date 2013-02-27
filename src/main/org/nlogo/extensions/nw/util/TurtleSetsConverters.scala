package org.nlogo.extensions.nw.util

import scala.collection.JavaConverters._

import org.nlogo.agent.{ Agent, ArrayAgentSet, Turtle, World }
import org.nlogo.api
import java.lang.{ Iterable => JIterable }
object TurtleSetsConverters {

  def toTurtleSets(turtleIterables: Traversable[JIterable[Turtle]], world: World): Seq[api.AgentSet] =
    turtleIterables.map(toTurtleSet(_, world))(collection.breakOut)

  def toTurtleSet(turtles: java.lang.Iterable[Turtle], world: World): api.AgentSet =
    toTurtleSet(turtles.asScala, world)

  def toTurtleSet(turtles: Traversable[Turtle], world: World): api.AgentSet = {
    val agents = turtles.toArray[Agent]
    new ArrayAgentSet(classOf[Turtle], agents, world)
  }

  def emptyTurtleSet(world: World) =
    new ArrayAgentSet(classOf[Turtle], Array[Agent](), world)

}