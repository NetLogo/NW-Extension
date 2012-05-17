package org.nlogo.extensions.nw

import org.apache.commons.collections15.Factory
import edu.uci.ics.jung.graph.Graph
import edu.uci.ics.jung.graph.SparseGraph
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.agent.World
import edu.uci.ics.jung.io.MatrixFile
import org.nlogo.agent.AgentSet
import scala.collection.JavaConverters._

object JungMatrix {

  def save(graph: Graph[Turtle, Link], filename: String) {
    new MatrixFile[Turtle, Link](
      null, // TODO: provide weight key (null means 1) 
      null, null, null // no factories needed for save
      ).save(graph, filename)
  }

  def load(filename: String, turtleBreed: AgentSet, linkBreed: AgentSet) {
    DummyGraph.importToNetLogo(
      new MatrixFile(
        null, // TODO: provide weight key (null means 1) 
        DummyGraph.factory, DummyGraph.vertexFactory, DummyGraph.edgeFactory)
        .load(filename),
      turtleBreed, linkBreed)
  }

}