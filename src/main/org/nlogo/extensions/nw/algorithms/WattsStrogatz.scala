package org.nlogo.extensions.nw.algorithms

/**
 * Created by marlontwyman on 7/3/15.
 */
import org.nlogo.agent
import org.nlogo.agent.AgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.createTurtle
import org.nlogo.util.MersenneTwisterFast


import java.lang.Math._

object WattsStrogatz {
  def generate (nbVertices: Int, nbDegrees: Int, prob: Double, rng: MersenneTwisterFast) = {
    if (nbDegrees>=nbVertices) throw new RuntimeException("There must be more vertices than degrees")
    if (prob<0||prob>1) throw new RuntimeException("The proability must be between 0 and 1")
    if (nbDegrees%2!=0) throw new RuntimeException("The number of degrees must be even")
    if (nbDegrees<2) throw new RuntimeException("The number of degrees must be greater than or equal to 2")
    

  }


}
