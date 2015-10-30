package org.nlogo.extensions.nw.util

import scala.collection.JavaConverters._

import org.nlogo.agent.{ Agent, ArrayAgentSet, Turtle, World }
import org.nlogo.core.AgentKind
import org.nlogo.api
import java.lang.{ Iterable => JIterable }

object TurtleSetsConverters {

  def toTurtleSets(turtleIterables: Traversable[JIterable[Turtle]], world: World, rng: java.util.Random): Seq[api.AgentSet] = {
    val turtleSets: Seq[api.AgentSet] =
      turtleIterables.map(toTurtleSet(_, world))(collection.breakOut)
    new scala.util.Random(rng).shuffle(turtleSets)
  }

  def toTurtleSet(turtles: java.lang.Iterable[Turtle], world: World): api.AgentSet =
    toTurtleSet(turtles.asScala, world)

  def toTurtleSet(turtles: Iterable[Turtle], world: World): api.AgentSet = {
    val agents = turtles.toArray[Agent]
    new ArrayAgentSet(AgentKind.Turtle, agents, world)
  }

  def emptyTurtleSet(world: World) =
    new ArrayAgentSet(AgentKind.Turtle, Array[Agent](), world)

}
