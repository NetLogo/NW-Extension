// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetLogoGraph
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToNetLogoAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.turtleCreatingCommand
import org.nlogo.extensions.nw.jung.io.Matrix

class SaveMatrix(getGraph: api.Context => NetLogoGraph)
  extends api.DefaultCommand {
  override def getSyntax = commandSyntax(Array(StringType))
  override def perform(args: Array[api.Argument], context: api.Context) {
    Matrix.save(getGraph(context).asJungGraph, args(0).getString)
  }
}

class LoadMatrix
  extends turtleCreatingCommand {
  override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    Matrix.load(
      filename = args(0).getString,
      turtleBreed = args(1).getAgentSet.requireTurtleBreed,
      linkBreed = args(2).getAgentSet.requireLinkBreed,
      rng = context.getRNG)
}