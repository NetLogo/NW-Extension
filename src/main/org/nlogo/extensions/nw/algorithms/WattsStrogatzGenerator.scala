package org.nlogo.extensions.nw.algorithms

/**
 * Created by marlontwyman on 7/3/15.
 */

import org.nlogo.agent
import org.nlogo.agent.AgentSet
import org.nlogo.api.Turtle
import org.nlogo.extensions.nw.NetworkExtensionUtil.createTurtle
import org.nlogo.util.MersenneTwisterFast

object WattsStrogatzGenerator {
  def generate(
                turtleBreed: AgentSet,
                linkBreed: AgentSet,
                nbTurtles: Int,
                neighborPerSide: Int,
                rewireProbability: Double,
                rng: MersenneTwisterFast): Seq[agent.Turtle] = {
    require(nbTurtles > 0,
      "A positive number of turtles must be specified.")
    require(rewireProbability >= 0 && rewireProbability <= 1.0,
      "The rewire probability must be between 0 and 1.")
    val turtles = Iterator.fill(nbTurtles)(createTurtle(turtleBreed, rng)).toIndexedSeq
    turtles.zipWithIndex.foreach{case (t: Turtle,i: Int) => t.heading(360.0*i/nbTurtles)}
    for {
      i <- 0 until nbTurtles //turtles
      j <- 1 to neighborPerSide //edges
    } {
      //create the links
      if (rng.nextDouble() < rewireProbability){
        linkBreed.world.linkManager.createLink(turtles(i),turtles(rng.nextInt(nbTurtles)),linkBreed) //randomly
      }
      else {
        linkBreed.world.linkManager.createLink(turtles(i),turtles((i+j) % nbTurtles),linkBreed) //next turtles over
      }
    }
    turtles
  }
}