package org.nlogo.extensions.nw.jung.io

import java.io.BufferedReader
import java.io.FileReader

import scala.Option.option2Iterable
import scala.annotation.implicitNotFound
import scala.collection.JavaConverters._

import org.nlogo.agent.Agent
import org.nlogo.agent.AgentSet
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import org.nlogo.extensions.nw.jung.createLink
import org.nlogo.extensions.nw.jung.createTurtle
import org.nlogo.extensions.nw.jung.factoryFor
import org.nlogo.extensions.nw.jung.transformer

import edu.uci.ics.jung
import edu.uci.ics.jung.io.graphml.AbstractMetadata
import edu.uci.ics.jung.io.graphml.EdgeMetadata
import edu.uci.ics.jung.io.graphml.GraphMLReader2
import edu.uci.ics.jung.io.graphml.GraphMetadata
import edu.uci.ics.jung.io.graphml.Key
import edu.uci.ics.jung.io.graphml.Metadata.MetadataType
import edu.uci.ics.jung.io.graphml.NodeMetadata

object GraphMLImport {

  trait GraphElement {
    val metadata: AbstractMetadata
    def id: String
  }

  case class Vertex(metadata: NodeMetadata) extends GraphElement {
    def id = metadata.getId
  }

  case class Edge(metadata: EdgeMetadata) extends GraphElement {
    def id = metadata.getId
  }

  object Attribute {
    def apply(name: String, attributeType: String, value: String) =
      new Attribute(name.toUpperCase, attributeType.toUpperCase, value)
  }
  class Attribute private (
    val name: String,
    val attributeType: String,
    val value: String) {
    def valueObject: Any =
      try {
        attributeType match {
          case "BOOLEAN" => value.toBoolean
          case "INT"     => value.toDouble
          case "LONG"    => value.toDouble
          case "FLOAT"   => value.toDouble
          case "DOUBLE"  => value.toDouble
          case _         => value // anything else stays a string
        }
      } catch {
        // If anything fails, we return the value as a string.
        // TODO: use a proper XML parser eventually! NP 2013-02-15
        case e: Exception => value
      }
  }

  def attributes(e: GraphElement, keys: Seq[Key]): Seq[Attribute] = {
    val properties = e.metadata.getProperties.asScala
    Attribute("ID", "STRING", e.id) +:
      (for {
        key <- keys
        value <- properties.get(key.getAttributeName).orElse(Option(key.defaultValue))
      } yield Attribute(key.getAttributeName, key.getAttributeType, value))
  }

  private def createAgents[E <: GraphElement, A <: Agent](
    elements: Iterable[E], keys: Seq[Key])(create: E => A): Map[E, A] =
    elements.map { elem =>
      val agent = create(elem)
      attributes(elem, keys).foreach { a =>
        try {
          agent.setTurtleOrLinkVariable(a.name, a.valueObject)
        } catch {
          case e: IllegalStateException => // Variable does not exist - move on
          case e: Exception             => throw new ExtensionException(e)
        }
      }
      elem -> agent
    }(scala.collection.breakOut)

  def load(fileName: String, turtleBreed: AgentSet, linkBreed: AgentSet, rng: java.util.Random): Iterator[Turtle] = {
    if (org.nlogo.workspace.AbstractWorkspace.isApplet)
      throw new ExtensionException("Cannot load GraphML file when in applet mode.")
    try {
      val fileReader = new BufferedReader(new FileReader(fileName))
      val graphFactory = factoryFor[Vertex, Edge](linkBreed)
      val graphTransformer = transformer { _: GraphMetadata => graphFactory.create }

      val graphReader =
        new GraphMLReader2[jung.graph.Graph[Vertex, Edge], Vertex, Edge](
          fileReader,
          graphTransformer,
          transformer(Vertex.apply),
          transformer(Edge.apply),
          transformer(_ => Edge(null)))

      val graph = graphReader.readGraph()

      val keyMap: Map[MetadataType, Seq[Key]] =
        graphReader
          .getGraphMLDocument.getKeyMap.entrySet()
          .asScala.map(entry => entry.getKey() -> entry.getValue().asScala).toMap

      val turtles: Map[Vertex, Turtle] =
        createAgents(graph.getVertices.asScala, keyMap(MetadataType.NODE)) {
          _ => createTurtle(turtleBreed, rng)
        }

      createAgents(graph.getEdges.asScala, keyMap(MetadataType.EDGE)) {
        e: Edge => createLink(turtles, graph.getEndpoints(e), linkBreed)
      }

      turtles.valuesIterator

    } catch {
      case e: Exception => throw new ExtensionException(e)
    }
  }
}