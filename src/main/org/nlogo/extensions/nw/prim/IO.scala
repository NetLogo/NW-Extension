package org.nlogo.extensions.nw.prim

import java.awt.Color
import java.io.File

import org.gephi.data.attributes.`type`.DynamicType
import org.gephi.io.importer.api.EdgeDraft.EdgeType
import org.gephi.io.importer.api.{EdgeDraftGetter, ImportController, NodeDraftGetter}
import org.nlogo.agent.{Link, Turtle, AgentSet}
import org.nlogo.api
import org.nlogo.api.AgentVariableNumbers._
import org.nlogo.api.{ExtensionException, LogoList}
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil._
import org.nlogo.nvm.ExtensionContext
import org.openide.util.Lookup

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

class Load extends TurtleAskingCommand {
  private type JDouble = java.lang.Double

  override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType))

  override def perform(args: Array[api.Argument], context: api.Context) = {
    val importer = Lookup.getDefault.lookup(classOf[ImportController])
    val ws = context.asInstanceOf[ExtensionContext].workspace
    val world = ws.world
    val breeds = world.program.breeds.asScala
    val turtlesOwn = world.program.turtlesOwn
    val linksOwn = world.program.linksOwn
    val fm = ws.fileManager
    val file = new File(fm.attachPrefix(args(0).getString))
    val turtleBreed = args(1).getAgentSet.requireTurtleBreed
    val linkBreed = args(2).getAgentSet.requireLinkBreed

    val unloader = importer.importFile(file).getUnloader
    val nodes: Iterable[NodeDraftGetter] = unloader.getNodes.asScala
    val edges: Iterable[EdgeDraftGetter] = unloader.getEdges.asScala

    val nodeToTurtle: Map[NodeDraftGetter, Turtle] = nodes zip nodes.map {
      node => {
        val attrs: Map[String, AnyRef] = node.getAttributeRow.getValues.filter(v => v.getValue != null).map { v =>
          println(v.getColumn.getTitle -> v.getValue)
          v.getColumn.getTitle.toUpperCase -> convertAttribute(v.getValue)
        }.toMap
        val breed: AgentSet = attrs.get("BREED").collect{case s: String => s.toUpperCase}
                                                .flatMap(s => breeds.get(s))
                                                .collect{case b: AgentSet => b}
                                                .getOrElse(turtleBreed)
        val turtle = createTurtle(breed, ws.mainRNG)
        Option(node.getLabel)      foreach (l => turtle.setTurtleVariable(VAR_LABEL, l))
        Option(node.getLabelColor) foreach (c => turtle.setTurtleVariable(VAR_LABELCOLOR, convertColor(c)))
        Option(node.getColor)      foreach (c => turtle.setTurtleVariable(VAR_COLOR, convertColor(c)))
        // Note that node's have a getSize. This does not correspond to the `size` attribute in files so should not be
        // used. BCH 1/21/2015
        //val values = node.getAttributeRow.getValues map (x => convertAttribute(x.getValue))
        (attrs - "BREED") foreach { case (k: String, v: AnyRef) =>
          if (turtlesOwn.contains(k))
            turtle.setTurtleOrLinkVariable(k, v)
          else if (turtle.ownsVariable(k))
            turtle.setBreedVariable(k, v)
        }
        turtle
      }
    } toMap

    val badEdges: ArrayBuffer[EdgeDraftGetter] = ArrayBuffer()
    val edgesToLinks: Map[EdgeDraftGetter, Seq[Link]] = edges zip edges.map { edge =>
      val source = nodeToTurtle(edge.getSource)
      val target = nodeToTurtle(edge.getTarget)
      // There are three gephi edge types: directed, undirected, and mutual. Mutual is pretty much just indicating that
      // and edge goes both ways/there are two edges in either direction, so we treat it as either. BCH 1/22/2015
      val gephiDirected = edge.getType == EdgeType.DIRECTED
      val gephiUndirected = edge.getType == EdgeType.UNDIRECTED
      if (linkBreed.isDirected == linkBreed.isUndirected) {
        linkBreed.setDirected(gephiDirected)
      } else if ((linkBreed.isDirected && gephiUndirected) || (linkBreed.isUndirected && gephiDirected)) {
        badEdges.append(edge)
      }

      val links = List(world.linkManager.createLink(source, target, linkBreed)) ++ {
        if (linkBreed.isDirected && edge.getType == EdgeType.MUTUAL)
          Some(world.linkManager.createLink(target, source, linkBreed))
        else
          None
      }
      links
    } toMap

    askTurtles(nodeToTurtle.values, context)

    if(badEdges.nonEmpty) {
      val edgesList = badEdges.map(e => e.getSource.getId + "->" + e.getTarget.getId).mkString(", ")
      val errorMsg =
        "The following edges had a directedness different than their assigned breed. They have been given " +
        "the directedness of their breed. If you wish to ignore this error, wrap this command in a CAREFULLY:"
      throw new ExtensionException(errorMsg + " " + edgesList)
    }
  }

  private def convertColor(c: Color): LogoList = {
    val l = LogoList(c.getRed.toDouble: JDouble,
                     c.getGreen.toDouble: JDouble,
                     c.getBlue.toDouble: JDouble)
    if (c.getAlpha != 255) l.lput(c.getAlpha.toDouble: JDouble)
    l
  }

  private def convertAttribute(o: Any): AnyRef = o match {
    case n: java.lang.Number => n.doubleValue: JDouble
    case b: java.lang.Boolean => b
    case c: java.util.Collection[_] => LogoList.fromIterator(c.asScala.map(x => convertAttribute(x)).iterator)
    // There may be a better handling of dynamic values, but this seems good enough for now. BCH 1/21/2015
    case d: DynamicType[_] => LogoList.fromIterator(d.getValues.asScala.map(x => convertAttribute(x)).iterator)
    case a: Array[_] => LogoList.fromIterator(a.map(x => convertAttribute(x)).iterator)
    case x => x.toString
  }
}

