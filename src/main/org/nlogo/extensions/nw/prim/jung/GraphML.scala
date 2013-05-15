// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.prim.jung

import scala.annotation.implicitNotFound

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.turtleCreatingCommand
import org.nlogo.extensions.nw.jung.io.GraphMLExport
import org.nlogo.extensions.nw.jung.io.GraphMLImport

class SaveGraphML(getGraphContext: api.World => GraphContext)
  extends api.DefaultCommand {
  override def getSyntax = commandSyntax(Array(StringType))
  override def perform(args: Array[api.Argument], context: api.Context) {
    GraphMLExport.save(getGraphContext(context.getAgent.world), args(0).getString)
  }
}

class LoadGraphML extends turtleCreatingCommand {
  override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType))
  def createTurtles(args: Array[api.Argument], context: api.Context) =
    GraphMLImport.load(
      fileName = args(0).getString,
      turtleBreed = args(1).getAgentSet.requireTurtleBreed,
      linkBreed = args(2).getAgentSet.requireLinkBreed,
      rng = context.getRNG)
}