package org.nlogo.extensions.nw.prim

import java.io.File

import org.nlogo.agent.World
import org.nlogo.api
import org.nlogo.core.Syntax._
import org.nlogo.nvm.ExtensionContext

import org.nlogo.extensions.nw.GraphContextProvider
import org.nlogo.extensions.nw.NetworkExtensionUtil._
import org.nlogo.extensions.nw.gephi.{ GephiExport, GephiImport, GephiUtils }

class Load extends TurtleAskingCommand {
  override def getSyntax = commandSyntax(
    right = List(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType),
    blockAgentClassString = Some("-T--"))
  override def perform(args: Array[api.Argument], context: api.Context) = GephiUtils.withNWLoaderContext {
    implicit val world = context.world.asInstanceOf[World]
    val ws = context.asInstanceOf[ExtensionContext].workspace
    val turtleBreed = args(1).getAgentSet.requireTurtleBreed
    val linkBreed = args(2).getAgentSet.requireLinkBreed
    val file = new File(ws.fileManager.attachPrefix(args(0).getString))
    GephiImport.load(file, world, turtleBreed, linkBreed, askTurtles(context))
  }
}

class LoadFileType(extension: String) extends TurtleAskingCommand {
  override def getSyntax = commandSyntax(right = List(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType), blockAgentClassString = Some("-T--"))
  override def perform(args: Array[api.Argument], context: api.Context) = GephiUtils.withNWLoaderContext {
    implicit val world = context.world.asInstanceOf[World]
    val ws = context.asInstanceOf[ExtensionContext].workspace
    val turtleBreed = args(1).getAgentSet.requireTurtleBreed
    val linkBreed = args(2).getAgentSet.requireLinkBreed
    val file = new File(ws.fileManager.attachPrefix(args(0).getString))
    GephiImport.load(file, world, turtleBreed, linkBreed, askTurtles(context), extension)
  }
}

class LoadFileTypeDefaultBreeds(extension: String) extends TurtleAskingCommand {
  override def getSyntax = commandSyntax(right = List(StringType, CommandBlockType | OptionalType), blockAgentClassString = Some("-T--"))
  override def perform(args: Array[api.Argument], context: api.Context) = GephiUtils.withNWLoaderContext {
    val world = context.world.asInstanceOf[World]
    val ws = context.asInstanceOf[ExtensionContext].workspace
    val file = new File(ws.fileManager.attachPrefix(args(0).getString))
    GephiImport.load(file, world, world.turtles, world.links, askTurtles(context), extension)
  }
}

class Save(gcp: GraphContextProvider) extends api.Command {
  override def getSyntax = commandSyntax(right = List(StringType))
  override def perform(args: Array[api.Argument], context: api.Context) = GephiUtils.withNWLoaderContext {
    val world = context.getAgent.world.asInstanceOf[World]
    val workspace = context.asInstanceOf[ExtensionContext].workspace
    val fm = workspace.fileManager
    val file = new File(fm.attachPrefix(args(0).getString))
    GephiExport.save(gcp.getGraphContext(world), world, file)
  }
}

class SaveFileType(gcp: GraphContextProvider, extension: String) extends api.Command {
  override def getSyntax = commandSyntax(right = List(StringType))
  override def perform(args: Array[api.Argument], context: api.Context) = GephiUtils.withNWLoaderContext {
    val world = context.getAgent.world.asInstanceOf[World]
    val workspace = context.asInstanceOf[ExtensionContext].workspace
    val fm = workspace.fileManager
    val file = new File(fm.attachPrefix(args(0).getString))
    GephiExport.save(gcp.getGraphContext(world), world, file, extension)
  }
}
