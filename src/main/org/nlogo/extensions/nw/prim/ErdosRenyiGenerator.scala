// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.ExtensionException
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.turtleCreatingCommand
import org.nlogo.extensions.nw.algorithms

class ErdosRenyiGenerator extends turtleCreatingCommand {
  override def getSyntax = commandSyntax(
    Array(TurtlesetType, LinksetType, NumberType, NumberType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) = {
    val turtleBreed = args(0).getAgentSet.requireTurtleBreed
    val linkBreed = args(1).getAgentSet.requireLinkBreed
    val nbTurtles = getIntValueWithMinimum(args(2), 1)
    val connexionProbability = args(3).getDoubleValue
    if (!(connexionProbability >= 0 && connexionProbability <= 1.0))
      throw new ExtensionException("The connexion probability must be between 0 and 1.")
    algorithms.ErdosRenyiGenerator.generate(
      turtleBreed, linkBreed,
      nbTurtles, connexionProbability,
      context.getRNG)
  }
}