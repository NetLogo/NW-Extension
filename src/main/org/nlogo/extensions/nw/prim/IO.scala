package org.nlogo.extensions.nw.prim

import java.awt.Color
import java.io.{File, FileReader, IOException}

import org.gephi.data.attributes.`type`.DynamicType
import org.gephi.data.attributes.api.AttributeRow
import org.gephi.io.importer.api.EdgeDraft.EdgeType
import org.gephi.io.importer.api.{EdgeDraftGetter, ImportController, NodeDraftGetter}
import org.nlogo.agent.{Agent, Turtle, AgentSet, World}
import org.nlogo.api
import org.nlogo.api.{ExtensionException, LogoList}
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtensionUtil._
import org.nlogo.extensions.nw.gephi.GephiUtils
import org.nlogo.nvm.ExtensionContext
import org.openide.util.Lookup

import scala.collection.JavaConverters._

class Load extends TurtleAskingCommand {
  private type JDouble = java.lang.Double

  override def getSyntax = commandSyntax(Array(StringType, TurtlesetType, LinksetType, CommandBlockType | OptionalType))

  override def perform(args: Array[api.Argument], context: api.Context) = GephiUtils.withNWLoaderContext {
    val importer = Lookup.getDefault.lookup(classOf[ImportController])
    val ws = context.asInstanceOf[ExtensionContext].workspace
    val world = ws.world
    val turtleBreeds = world.program.breeds.asScala.toMap
    val linkBreeds = world.program.linkBreeds.asScala.toMap
    val fm = ws.fileManager
    val turtleBreed = args(1).getAgentSet.requireTurtleBreed
    val linkBreed = args(2).getAgentSet.requireLinkBreed

    val file = new File(fm.attachPrefix(args(0).getString))
    val parser = importer.getFileImporter(file)
    val unloader = try using(new FileReader(file))(r => importer.importFile(r, parser).getUnloader)
                   catch { case e: IOException => throw new ExtensionException(e) }
    val nodes: Iterable[NodeDraftGetter] = unloader.getNodes.asScala
    val edges: Iterable[EdgeDraftGetter] = unloader.getEdges.asScala

    val nodeToTurtle: Map[NodeDraftGetter, Turtle] = nodes zip nodes.map {
      node => {
        val attrs = getAttributes(node.getAttributeRow) ++
                    pair("LABEL", node.getLabel) ++
                    pair("LABEL-COLOR", node.getLabelColor) ++
                    pair("COLOR", node.getColor)
        // Note that node's have a getSize. This does not correspond to the `size` attribute in files so should not be
        // used. BCH 1/21/2015

        val breed = getBreed(attrs, turtleBreeds).getOrElse(turtleBreed)
        val turtle = createTurtle(breed, ws.mainRNG)
        (attrs - "BREED") foreach (setAttribute(world, turtle) _).tupled
        turtle
      }
    } toMap

    val badEdges = edges.map { edge =>
      val source = nodeToTurtle(edge.getSource)
      val target = nodeToTurtle(edge.getTarget)
      // There are three gephi edge types: directed, undirected, and mutual. Mutual is pretty much just indicating that
      // and edge goes both ways/there are two edges in either direction, so we treat it as either. BCH 1/22/2015
      val attrs = getAttributes(edge.getAttributeRow) ++
                  pair("LABEL", edge.getLabel) ++
                  pair("LABEL-COLOR", edge.getLabelColor) ++
                  pair("COLOR", edge.getColor) ++
                  pair("WEIGHT", edge.getWeight.toDouble: java.lang.Double)

      val breed = getBreed(attrs, linkBreeds).getOrElse(linkBreed)

      val links = List(world.linkManager.createLink(source, target, breed)) ++ {
        if (breed.isDirected && edge.getType == EdgeType.MUTUAL)
          Some(world.linkManager.createLink(target, source, breed))
        else
          None
      }
      links foreach { l => (attrs - "BREED") foreach (setAttribute(world, l) _).tupled }

      val gephiDirected = edge.getType == EdgeType.DIRECTED
      val gephiUndirected = edge.getType == EdgeType.UNDIRECTED
      if (breed.isDirected == breed.isUndirected) {
        // This happens when the directedness of the default breed hasn't been set yet
        breed.setDirected(gephiDirected)
        None
      } else if ((breed.isDirected && gephiUndirected) || (breed.isUndirected && gephiDirected)) {
        Some(edge)
      } else {
        None
      }
    }.collect{case Some(e: EdgeDraftGetter) => e}

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

  private def pair(key: String, value: AnyRef): Option[(String, AnyRef)] =
    Option(value) map (v => key -> convertAttribute(v))

  private def getAttributes(row: AttributeRow): Map[String, AnyRef] =
    row.getValues.filter(v => v.getValue != null).map { v =>
      v.getColumn.getTitle.toUpperCase -> convertAttribute(v.getValue)
    }.toMap

  private def getBreed(attributes: Map[String, AnyRef], breeds: Map[String, AnyRef]): Option[AgentSet] =
    attributes.get("BREED").collect{case s: String => s.toUpperCase}
                          .flatMap(s => breeds.get(s))
                          .collect{case b: AgentSet => b}

  private def convertAttribute(o: Any): AnyRef = o match {
    case c: Color => convertColor(c)
    case n: java.lang.Number => n.doubleValue: JDouble
    case b: java.lang.Boolean => b
    case c: java.util.Collection[_] => LogoList.fromIterator(c.asScala.map(x => convertAttribute(x)).iterator)
    // There may be a better handling of dynamic values, but this seems good enough for now. BCH 1/21/2015
    case d: DynamicType[_] => LogoList.fromIterator(d.getValues.asScala.map(x => convertAttribute(x)).iterator)
    case a: Array[_] => LogoList.fromIterator(a.map(x => convertAttribute(x)).iterator)
    case x => x.toString
  }

  private def setAttribute(world: World, agent: Agent)(name: String, value: AnyRef): Unit = {
    val i = world.indexOfVariable(agent, name)
    if (i != -1) agent.setVariable(i, value)
  }
}

