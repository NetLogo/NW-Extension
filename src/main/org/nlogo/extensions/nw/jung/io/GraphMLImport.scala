package org.nlogo.extensions.nw.jung.io

import java.io.BufferedReader
import java.io.FileReader
import java.io.File
import java.util.Locale
import scala.Option.option2Iterable
import scala.collection.JavaConverters._
import org.nlogo.app.App
import org.nlogo.agent.Agent
import org.nlogo.agent.AgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.AgentException
import org.nlogo.api.ExtensionException
import org.nlogo.api
import org.nlogo.extensions.nw.NetworkExtensionUtil.{createTurtle, using}
import org.nlogo.extensions.nw.jung.createLink
import org.nlogo.extensions.nw.jung.sparseGraphFactory
import org.nlogo.extensions.nw.jung.transformer
import org.nlogo.util.MersenneTwisterFast
import edu.uci.ics.jung
import edu.uci.ics.jung.io.graphml.AbstractMetadata
import edu.uci.ics.jung.io.graphml.EdgeMetadata
import edu.uci.ics.jung.io.graphml.GraphMLReader2
import edu.uci.ics.jung.io.graphml.GraphMetadata
import edu.uci.ics.jung.io.graphml.Key
import edu.uci.ics.jung.io.graphml.Metadata.MetadataType
import edu.uci.ics.jung.io.graphml.NodeMetadata
import org.nlogo.agent.World

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
      new Attribute(
        name.toUpperCase(Locale.ENGLISH),
        attributeType.toUpperCase(Locale.ENGLISH),
        Option(value).getOrElse(""))
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
          case "STRING"  => value
          case _ =>
            // trial and errors for unknown types
            try value.toDouble
            catch {
              case _: Exception =>
                try value.toBoolean
                catch { case _: Exception => value } // string as a final resort
            }
        }
      } catch {
        // If anything fails, we return the value as a string.
        // TODO: use a proper XML parser eventually! NP 2013-02-15
        case e: Exception => value
      }
  }

  def attributes(e: GraphElement, keys: Seq[Key]): Seq[Attribute] = {
    val properties = e.metadata.getProperties.asScala
    val as = Attribute("ID", "STRING", e.id) +:
      (for {
        key <- keys
        value <- properties.get(key.getId).orElse(Option(key.defaultValue))
        attributeName = Option(key.getAttributeName).getOrElse(key.getId)
        attributeType = Option(key.getAttributeType).getOrElse("")
      } yield Attribute(attributeName, attributeType, value))
    as.sortBy(_.name != "BREED") // BREED first
  }

  private def setBreed(agent: Agent, breed: String) {
    val program = agent.world.program
    agent match {
      case t: Turtle =>
        for (breedAgentSet <- program.breeds.asScala.get(breed))
          t.setTurtleOrLinkVariable("BREED", breedAgentSet)
      case l: Link =>
        for (breedAgentSet <- program.linkBreeds.asScala.get(breed))
          l.setTurtleOrLinkVariable("BREED", breedAgentSet)
    }
  }

  private def setAgentVariable(agent: Agent, attribute: Attribute) {
    if (attribute.name != "WHO") // don' try to set WHO
      try {
        val program = agent.world.program
        agent match {
          case l: Link =>
            attribute.name match {
              case "BREED" => // Breed is set separately
              case v if program.linksOwn.indexOf(v) != -1 =>
                agent.setTurtleOrLinkVariable(v, attribute.valueObject)
              case v =>
                agent.setLinkBreedVariable(v, attribute.valueObject)
            }
          case t: Turtle =>
            attribute.name match {
              case "BREED" => // Breed is set separately
              case v if program.turtlesOwn.indexOf(v) != -1 =>
                agent.setTurtleOrLinkVariable(v, attribute.valueObject)
              case v =>
                agent.setBreedVariable(v, attribute.valueObject)
            }
        }
      } catch {
        case e: AgentException => // Variable just does not exist - move on
        case e: Exception      => throw new ExtensionException(e)
      }
  }

  private def createAgents[E <: GraphElement, A <: Agent](elements: Iterable[E],
                                                          keys: Seq[Key],
                                                          defaultBreed: AgentSet,
                                                          breeds: String => AgentSet)
                                                         (create: (E, AgentSet) => A): Map[E, A] =
    elements.map { elem =>
      val attrs = attributes(elem, keys)
      val breed = attrs.find(_.name == "BREED").map{
        _.valueObject.toString.toUpperCase(Locale.ENGLISH)
      }.flatMap(name => Option(breeds(name))).getOrElse(defaultBreed)
      val agent = create(elem, breed)
      attrs.foreach { setAgentVariable(agent, _) }
      elem -> agent
    }(scala.collection.breakOut)

  def load(fileName: String, world: World, rng: MersenneTwisterFast): Iterator[Turtle] = {
    if (org.nlogo.workspace.AbstractWorkspace.isApplet)
      throw new ExtensionException("Cannot load GraphML file when in applet mode.")
    try {
      val graphFactory = sparseGraphFactory[Vertex, Edge]
      val graphTransformer = transformer { _: GraphMetadata => graphFactory.create }

      using {
        new GraphMLReader2[jung.graph.Graph[Vertex, Edge], Vertex, Edge](
          new BufferedReader(new FileReader(fileName)),
          graphTransformer,
          transformer(Vertex.apply),
          transformer(Edge.apply),
          transformer(_ => Edge(null)))
      } { graphReader =>
        val graph = graphReader.readGraph()

        val keyMap: Map[MetadataType, Seq[Key]] =
          graphReader
            .getGraphMLDocument.getKeyMap.entrySet()
            .asScala.map(entry => entry.getKey -> entry.getValue.asScala).toMap

        val turtles: Map[Vertex, Turtle] =
          createAgents(graph.getVertices.asScala, keyMap(MetadataType.NODE), world.turtles, world.getBreed) {
            (_, breed) => createTurtle(breed, rng)
          }

        createAgents(graph.getEdges.asScala, keyMap(MetadataType.EDGE), world.links, world.getLinkBreed) {
          (e: Edge, breed) => createLink(turtles, graph.getEndpoints(e), breed)
        }

        turtles.valuesIterator
      }
    } catch {
      case e: Exception => throw new ExtensionException(e)
    }
  }
}
