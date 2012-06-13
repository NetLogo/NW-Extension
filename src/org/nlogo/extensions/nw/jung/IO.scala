package org.nlogo.extensions.nw.jung

import org.nlogo.agent.AgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import edu.uci.ics.jung
import java.util.Random

object Matrix {

  def save(graph: jung.graph.Graph[Turtle, Link], filename: String) {
    new jung.io.MatrixFile[Turtle, Link](
      null, // TODO: provide weight key (null means 1) 
      null, null, null // no factories needed for save
      ).save(graph, filename)
  }

  def load(filename: String, turtleBreed: AgentSet, linkBreed: AgentSet, rng: Random) {
    DummyGraph.importToNetLogo(
      new jung.io.MatrixFile(
        null, // TODO: provide weight key (null means 1) 
        DummyGraph.factory, DummyGraph.vertexFactory, DummyGraph.edgeFactory)
        .load(filename),
      turtleBreed, linkBreed, rng)
  }

}