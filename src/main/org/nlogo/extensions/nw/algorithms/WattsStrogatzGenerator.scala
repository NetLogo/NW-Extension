package org.nlogo.extensions.nw.algorithms

import org.nlogo.agent
import org.nlogo.agent.{ AgentSet, World, Turtle }
import org.nlogo.extensions.nw.NetworkExtensionUtil.createTurtle
import org.nlogo.api.MersenneTwisterFast
import scala.collection.mutable

object WattsStrogatzGenerator {
  def generate(
    world: World,
    turtleBreed: AgentSet,
    linkBreed: AgentSet,
    nbTurtles: Int,
    neighborhoodSize: Int,
    rewireProbability: Double,
    rng: MersenneTwisterFast): Seq[agent.Turtle] = {

    val turtles = (0 until nbTurtles).map { i =>
      val t = world.createTurtle(turtleBreed)
      t.colorDouble((i % 14) * 10.0 + 5.0)
      t.heading((360.0 * i) / nbTurtles)
      t
    }

    val adjMap: Map[Turtle, mutable.Set[Turtle]] = turtles.zipWithIndex.map { case (t: Turtle, i: Int) =>
      val targets: mutable.Set[Turtle] = (1 to neighborhoodSize).map(j => turtles((i + j) % nbTurtles))(collection.breakOut)
        t -> targets
    }(collection.breakOut)
    println(adjMap.values.map(_.size).sum)

    for {
      (source, i) <- turtles.zipWithIndex
      neighbor <- 1 to neighborhoodSize
    } {
      val target = turtles((i + neighbor) % nbTurtles)
      val realTarget = if (rng.nextDouble < rewireProbability) {
        adjMap(source).remove(target)
        val avail = turtles.filter( t => t != source && !adjMap(source).contains(t) && !adjMap(t).contains(source))
        val newTarget = avail(rng.nextInt(avail.size))
        adjMap(source).add(newTarget)
        newTarget
      } else target
      world.linkManager.createLink(source, realTarget, linkBreed)
    }
    println(adjMap.values.map(_.size).sum)
    turtles
  }
}
