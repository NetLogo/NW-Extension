package org.nlogo.extensions.nw.util

import scala.collection.JavaConverters._

import org.nlogo.agent.{ Agent, AgentSet, Turtle, World }
import org.nlogo.core.AgentKind
import org.nlogo.api
import java.lang.{ Iterable => JIterable }

object TurtleSetsConverters {

  def toTurtleSets(turtleIterables: Traversable[JIterable[Turtle]], rng: java.util.Random): Seq[api.AgentSet] = {
    val turtleSets: Seq[api.AgentSet] =
      turtleIterables.map(toTurtleSet)(collection.breakOut)
    new scala.util.Random(rng).shuffle(turtleSets)
  }

  def toTurtleSet(turtles: java.lang.Iterable[Turtle]): api.AgentSet =
    toTurtleSet(turtles.asScala)

  def toTurtleSet(turtles: Iterable[Turtle]): api.AgentSet =
    AgentSet.fromArray(AgentKind.Turtle, turtles.toArray[Agent])

  def emptyTurtleSet(world: World) =
    AgentSet.fromArray(AgentKind.Turtle, Array.empty[Agent])
}
