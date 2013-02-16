package org.nlogo.extensions.nw

import org.apache.commons.collections15.Transformer
import edu.uci.ics.jung.{ graph => jg }
import org.nlogo.agent.AgentSet
import org.apache.commons.collections15.Factory
import org.nlogo.agent.Turtle
import edu.uci.ics.jung.graph.util.Pair

package object jung {

  def transformer[A, B](f: A => B) =
    new Transformer[A, B]() {
      override def transform(a: A): B = f(a)
    }

  def factory[A](f: => A) =
    new Factory[A]() {
      override def create = f
    }

  def factoryFor[V, E](linkBreed: AgentSet): Factory[jg.Graph[V, E]] =
    if (linkBreed.isDirected)
      factory { new jg.DirectedSparseGraph[V, E] }
    else
      factory { new jg.UndirectedSparseGraph[V, E] }

  def directedFactory[V, E] = jg.DirectedSparseGraph.getFactory[V, E]
  def undirectedFactory[V, E] = jg.UndirectedSparseGraph.getFactory[V, E]

  def createTurtle(turtleBreed: AgentSet, rng: java.util.Random) =
    turtleBreed.world.createTurtle(
      turtleBreed,
      rng.nextInt(14), // color
      rng.nextInt(360)) // heading

  def createLink[V](turtles: Map[V, Turtle], endPoints: Pair[V], linkBreed: AgentSet) =
    linkBreed.world.linkManager.createLink(
      turtles(endPoints.getFirst),
      turtles(endPoints.getSecond),
      linkBreed)

}