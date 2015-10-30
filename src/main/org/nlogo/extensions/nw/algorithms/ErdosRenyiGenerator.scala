package org.nlogo.extensions.nw.algorithms

import org.nlogo.agent
import org.nlogo.agent.AgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.createTurtle
import org.nlogo.api.MersenneTwisterFast

object ErdosRenyiGenerator {
  def generate(
    turtleBreed: AgentSet,
    linkBreed: AgentSet,
    nbTurtles: Int,
    connexionProbability: Double,
    rng: MersenneTwisterFast): Seq[agent.Turtle] = {
    require(nbTurtles > 0,
      "A positive number of turtles must be specified.")
    require(connexionProbability >= 0 && connexionProbability <= 1.0,
      "The connexion probability must be between 0 and 1.")
    val turtles = Iterator.fill(nbTurtles)(createTurtle(turtleBreed, rng)).toIndexedSeq
    def jRange(i: Int) =
      if (linkBreed.isDirected) (0 until nbTurtles) filter { _ != i }
      else (i + 1) until nbTurtles
    for {
      i <- 0 until nbTurtles
      j <- jRange(i)
      if rng.nextDouble() < connexionProbability
    } linkBreed.world.linkManager.createLink(turtles(i), turtles(j), linkBreed)
    turtles
  }
}