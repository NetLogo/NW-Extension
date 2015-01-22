package org.nlogo.extensions.nw.prim

import java.awt.Color
import java.io.File

import org.gephi.io.importer.api.{EdgeDraftGetter, ImportController, NodeDraftGetter}
import org.nlogo.agent.{Link, Turtle}
import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.api.{Context, LogoList, AgentVariableNumbers}
import AgentVariableNumbers._
import org.nlogo.extensions.nw.NetworkExtensionUtil.{TurtleCreatingCommand, _}
import org.nlogo.extensions.nw.gephi.GephiUtils.withNWLoaderContext
import org.nlogo.nvm.ExtensionContext
import org.openide.util.Lookup

import scala.collection.JavaConverters._

class Load extends TurtleCreatingCommand {
  private type JDouble = java.lang.Double

  override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType))

  def createTurtles(args: Array[api.Argument], context: Context): TraversableOnce[Turtle] = withNWLoaderContext {
    val importer = Lookup.getDefault.lookup(classOf[ImportController])
    val ws = context.asInstanceOf[ExtensionContext].workspace
    val world = ws.world
    val fm = ws.fileManager
    val file = new File(fm.attachPrefix(args(0).getString))
    val turtleBreed = args(1).getAgentSet.requireTurtleBreed
    val linkBreed = args(2).getAgentSet.requireLinkBreed
    val unloader = importer.importFile(file).getUnloader
    val nodes: Iterable[NodeDraftGetter] = unloader.getNodes.asScala
    val edges: Iterable[EdgeDraftGetter] = unloader.getEdges.asScala

    val nodeToTurtle: Map[NodeDraftGetter, Turtle] = nodes zip nodes.map {
      node => {
        val turtle = createTurtle(turtleBreed, ws.mainRNG)
        Option(node.getLabel)      foreach (l => turtle.setTurtleVariable(VAR_LABEL, l))
        Option(node.getLabelColor) foreach (c => turtle.setTurtleVariable(VAR_LABELCOLOR, convertColor(c)))
        Option(node.getColor)      foreach (c => turtle.setTurtleVariable(VAR_COLOR, convertColor(c)))
        // Note that node's have a getSize. This does not correspond to the `size` attribute in files so should not be
        // used. BCH 1/21/2015
        turtle
      }
    } toMap

    val edgesToLinks: Map[EdgeDraftGetter, Link] = edges zip edges.map { edge =>
      val source = nodeToTurtle(edge.getSource)
      val target = nodeToTurtle(edge.getTarget)
      world.linkManager.createLink(source, target, linkBreed)
    } toMap

    nodeToTurtle.values
  }

  private def convertColor(c: Color): LogoList = {
    val l = LogoList(c.getRed.toDouble: JDouble,
                     c.getGreen.toDouble: JDouble,
                     c.getBlue.toDouble: JDouble)
    if (c.getAlpha != 255) l.lput(c.getAlpha.toDouble: JDouble)
    l
  }
}

