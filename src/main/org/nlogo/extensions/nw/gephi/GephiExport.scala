package org.nlogo.extensions.nw.gephi

import java.io.File

import org.nlogo.agent.{ AgentSet, Link, Turtle, World }
import org.nlogo.api
import org.nlogo.api._
import org.nlogo.core.Breed

import org.gephi.data.attributes.api.{ AttributeColumn, AttributeType, AttributeController }
import org.gephi.graph.api.GraphController
import org.gephi.io.exporter.api.ExportController
import org.gephi.io.exporter.plugin.{ ExporterCSV, ExporterGraphML }
import org.gephi.io.exporter.spi.Exporter
import org.gephi.project.api.ProjectController
import org.gephi.utils.longtask.spi.LongTask
import org.gephi.utils.progress.ProgressTicket
import org.openide.util.Lookup

import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.NetworkExtensionUtil._

object GephiExport {
  val exportController = GephiUtils.withNWLoaderContext {Lookup.getDefault.lookup(classOf[ExportController])}

  def save(context: GraphContext, world: World, file: File, extension: String): Unit = GephiUtils.withNWLoaderContext {
    save(context, world, file, exportController.getExporter(extension))
  }

  def save(context: GraphContext, world: World, file: File): Unit = GephiUtils.withNWLoaderContext {
    save(context, world, file, exportController.getFileExporter(file))
  }

  def save(context: GraphContext, world: World, file: File, exporter: Exporter) = GephiUtils.withNWLoaderContext {
    implicit val implicitWorld = world;

    if (exporter == null) {
      throw new ExtensionException("Unable to find exporter for " + file)
    } else if (exporter.isInstanceOf[ExporterCSV]) {
      throw new ExtensionException("Exporting CSV files is not supported.")
    } else if (exporter.isInstanceOf[ExporterGraphML]) {
      throw new ExtensionException("You must use nw:save-graphml to save graphml files.")
    }


    // Some exporters expect a progress ticker to be passed in :( BCH 5/8/2015
    exporter match {
      case lt: LongTask => lt.setProgressTicket(new ProgressTicket {
          def finish(): Unit = ()

          def finish(finishMessage: String): Unit = ()

          def getDisplayName: String = ""

          def progress(): Unit = ()

          def progress(workunit: Int): Unit = ()

          def progress(message: String): Unit = ()

          def progress(message: String, workunit: Int): Unit = ()

          def switchToIndeterminate(): Unit = ()

          def setDisplayName(newDisplayName: String): Unit = ()

          def start(): Unit = ()

          def start(workunits: Int): Unit = ()

          def switchToDeterminate(workunits: Int): Unit = ()
        } )
      case _ => // ignore
    }
    val program = world.program
    val projectController = Lookup.getDefault.lookup(classOf[ProjectController])
    projectController.newProject()
    val gephiWorkspace = projectController.getCurrentWorkspace
    val graphModel = Lookup.getDefault.lookup(classOf[GraphController]).getModel
    val attributeModel = Lookup.getDefault.lookup(classOf[AttributeController]).getModel
    val graph = (context.links.exists(_.isDirectedLink), context.links.exists(!_.isDirectedLink)) match {
      case (true, true) => graphModel.getMixedGraph
      case (true, false) => graphModel.getDirectedGraph
      case (false, true) => graphModel.getUndirectedGraph
      case _ => graphModel.getGraph
    }

    val nodeAttributes = attributeModel.getNodeTable

    val turtlesOwnAttributes: Map[String, AttributeColumn] = program.turtlesOwn.map { name =>
      val kind = getBestType(world.turtles.asIterable[Turtle].map(t => t.getTurtleOrLinkVariable(name)))
      name -> Option(nodeAttributes.getColumn(name)).getOrElse(nodeAttributes.addColumn(name, kind))
    }.toMap

    val breedsOwnAttributes: Map[AgentSet, Map[String, AttributeColumn]] = program.breeds.collect {
      case (breedName, breed: Breed) =>
        world.getBreed(breedName) -> breed.owns.map { name =>
          val kind = getBestType(world.getBreed(breedName).asIterable[Turtle].map(t => t.getBreedVariable(name)))
          name -> Option(nodeAttributes.getColumn(name)).getOrElse(nodeAttributes.addColumn(name, kind))
        }.toMap
    }.toMap

    val nodes = context.nodes.map { turtle =>
      val node = graphModel.factory.newNode(turtle.toString.split(" ").mkString("-"))
      turtlesOwnAttributes.foreach { case (name, col) =>
        node.getAttributes.setValue(col.getIndex, coerce(turtle.getTurtleOrLinkVariable(name), col.getType))
      }
      if (turtle.getBreed != world.turtles) {
        breedsOwnAttributes(turtle.getBreed).foreach { case (name, col) =>
          node.getAttributes.setValue(col.getIndex, coerce(turtle.getBreedVariable(name), col.getType))
        }
      }
      val color = api.Color.getColor(turtle.color())
      node.getNodeData.setColor(color.getRed / 255f, color.getGreen / 255f, color.getBlue / 255f)
      node.getNodeData.setAlpha(color.getAlpha / 255f)
      graph.addNode(node)
      turtle -> node
    }.toMap

    val edgeAttributes = attributeModel.getEdgeTable
    val linksOwnAttributes: Map[String, AttributeColumn] = program.linksOwn.map { name =>
      val kind = getBestType(world.links.asIterable[Link].map(l => l.getTurtleOrLinkVariable(name)))
      name -> Option(edgeAttributes.getColumn(name)).getOrElse(edgeAttributes.addColumn(name, kind))
    }.toMap

    val linkBreedsOwnAttributes: Map[AgentSet, Map[String, AttributeColumn]] = program.linkBreeds.collect {
      case (breedName, breed: Breed) =>
        val breedSet = world.getLinkBreed(breedName.toUpperCase)
        breedSet -> breed.owns.map { name =>
          lazy val kind = getBestType(breedSet.asIterable[Link].map(_.getLinkBreedVariable(name)))
          name -> Option(edgeAttributes.getColumn(name)).getOrElse(edgeAttributes.addColumn(name, kind))
        }.toMap
    }.toMap

    context.links.foreach { link =>
      val edge = graphModel.factory.newEdge(nodes(link.end1), nodes(link.end2), 1, link.isDirectedLink)
      linksOwnAttributes.foreach { case (name, col) =>
        edge.getAttributes.setValue(col.getIndex, coerce(link.getTurtleOrLinkVariable(name), col.getType))
      }
      if (link.getBreed != world.links) {
        linkBreedsOwnAttributes(link.getBreed).foreach { case (name, col) =>
          edge.getAttributes.setValue(col.getIndex, coerce(link.getLinkBreedVariable(name), col.getType))
        }
      }
      val color = api.Color.getColor(link.color())
      edge.getEdgeData.setColor(color.getRed / 255f, color.getGreen / 255f, color.getBlue / 255f)
      edge.getEdgeData.setAlpha(color.getAlpha / 255f)
      graph.addEdge(edge)
    }
    gephiWorkspace.add(graphModel)
    gephiWorkspace.add(attributeModel)
    exportController.exportFile(file, exporter)
  }

  private type JDouble = java.lang.Double
  private type JBoolean = java.lang.Boolean

  private def getBestType(values: Iterable[AnyRef]): AttributeType = {
    if (values.forall(_.isInstanceOf[Number])){
      AttributeType.DOUBLE
    } else if (values.forall(_.isInstanceOf[JBoolean])) {
      AttributeType.BOOLEAN
    } else {
      AttributeType.STRING
    }
  }

  private def coerce(value: AnyRef, kind: AttributeType): AnyRef =
    (value, kind) match {
      case (x: Number, AttributeType.DOUBLE)    => x.doubleValue: JDouble
      case (x: Number, AttributeType.FLOAT)     => x.doubleValue: JDouble
      case (b: JBoolean, AttributeType.BOOLEAN) => b
      // For Strings, we want to keep the escaping but ditch the surrounding quotes. BCH 1/26/2015
      case (s: String, AttributeType.STRING)    => Dump.logoObject(s, readable = true, exporting = false).drop(1).dropRight(1)
      case (s: AnyRef, AttributeType.STRING)    => Dump.logoObject(s, readable = true, exporting = false)
      case (o: AnyRef, attributeType)           =>
        throw new ExtensionException(s"Could not coerce ${Dump.logoObject(o, readable = true, exporting = false)} to $attributeType")
    }

}
