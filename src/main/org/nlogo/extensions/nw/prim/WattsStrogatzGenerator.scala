package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api
import org.nlogo.api.ExtensionException
import org.nlogo.api.ExtensionException
import org.nlogo.api.Syntax._
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleCreatingCommand
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleCreatingCommand
import org.nlogo.extensions.nw.algorithms
import org.nlogo.extensions.nw.algorithms
/**
 * Created by marlontwyman on 7/30/15.
 */
class WattsStrogatzGenerator extends TurtleCreatingCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, NumberType, NumberType, CommandBlockType | OptionalType)) //What are we inputting in Netlogo? This goes here
  def createTurtles(args: Array[api.Argument], context: api.Context) = {
    val turtleBreed = args(0).getAgentSet.requireTurtleBreed
    val linkBreed = args(1).getAgentSet.requireLinkBreed
    val nbTurtles = getIntValueWithMinimum(args(2), 1)
    val neighborsPerSide = getIntValueWithMinimum(args(3),1)
    if (neighborsPerSide > math.ceil(neighborsPerSide/2))
      throw new ExtensionException("There can only be half of the number of turtles on a side.")
    val rewireProbability = args(4).getDoubleValue
    if (!(rewireProbability >= 0 && rewireProbability <= 1.0))
      throw new ExtensionException("The rewire probability must be between 0 and 1.")
    algorithms.WattsStrogatzGenerator.generate(
      turtleBreed, linkBreed,
      nbTurtles, neighborsPerSide, rewireProbability,
      context.getRNG)
  }
}

