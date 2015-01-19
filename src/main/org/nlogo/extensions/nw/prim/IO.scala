package org.nlogo.extensions.nw.prim

import java.io.File

import org.gephi.io.importer.api.{EdgeDraftGetter, ImportController, NodeDraftGetter}
import org.nlogo.agent.{Link, Turtle}
import org.nlogo.api
import org.nlogo.api.Context
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil.{TurtleCreatingCommand, _}
import org.nlogo.nvm.ExtensionContext
import org.openide.util.Lookup
import org.nlogo.extensions.nw.gephi.GephiUtils.withNWLoaderContext

import scala.collection.JavaConverters._

class Load extends TurtleCreatingCommand {
  override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType))

  def createTurtles(args: Array[api.Argument], context: Context): TraversableOnce[Turtle] = withNWLoaderContext {
    val importer = Lookup.getDefault.lookup(classOf[ImportController])
    val ws = context.asInstanceOf[ExtensionContext].workspace
    val world = ws.world
    val fm = ws.fileManager
    val file = new File(fm.attachPrefix(args(0).getString))
    val turtleBreed = args(1).getAgentSet.requireTurtleBreed
    val linkBreed = args(2).getAgentSet.requireLinkBreed
    val container = importer.importFile(file)
    val nodes: Iterable[NodeDraftGetter] = container.getUnloader.getNodes.asScala
    val edges: Iterable[EdgeDraftGetter] = container.getUnloader.getEdges.asScala

    val nodeToTurtle: Map[NodeDraftGetter, Turtle] = nodes zip nodes.map {
      node => createTurtle(turtleBreed, ws.mainRNG): Turtle
    } toMap

    val edgesToLinks: Map[EdgeDraftGetter, Link] = edges zip edges.map { edge =>
      val source = nodeToTurtle(edge.getSource)
      val target = nodeToTurtle(edge.getTarget)
      world.linkManager.createLink(source, target, linkBreed)
    } toMap

    nodeToTurtle.values
  }
}

