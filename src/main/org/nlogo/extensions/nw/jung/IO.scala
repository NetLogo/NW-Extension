// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jung

import org.nlogo.agent.AgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import edu.uci.ics.jung
import java.util.Random
import org.nlogo.workspace.AbstractWorkspace
import org.nlogo.api.ExtensionException

object Matrix {

  def save(graph: jung.graph.Graph[Turtle, Link], filename: String) {
    if (org.nlogo.workspace.AbstractWorkspace.isApplet)
      throw new ExtensionException("Cannot save matrix file when in applet mode.")
    new jung.io.MatrixFile[Turtle, Link](
      null, // TODO: provide weight key (null means 1) (issue #19) 
      null, null, null // no factories needed for save
      ).save(graph, filename)
  }

  def load(filename: String, turtleBreed: AgentSet, linkBreed: AgentSet, rng: Random) = {
    if (org.nlogo.workspace.AbstractWorkspace.isApplet)
      throw new ExtensionException("Cannot load matrix file when in applet mode.")
    val matrixFile = new jung.io.MatrixFile(
      null, // TODO: provide weight key (null means 1) (issue #19) 
      factoryFor(linkBreed), DummyGraph.vertexFactory, DummyGraph.edgeFactory)
    val graph = matrixFile.load(filename)
    DummyGraph.importToNetLogo(graph, turtleBreed, linkBreed, rng)
  }
}
