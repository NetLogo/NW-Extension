// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jung.io

import org.nlogo.agent.AgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import org.nlogo.extensions.nw.jung.DummyGraph
import org.nlogo.extensions.nw.jung.factoryFor
import org.nlogo.util.MersenneTwisterFast

import edu.uci.ics.jung
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations

object Matrix {

  def save(graph: jung.graph.Graph[Turtle, Link], filename: String) {
    if (org.nlogo.workspace.AbstractWorkspace.isApplet)
      throw new ExtensionException("Cannot save matrix file when in applet mode.")
    /* This is almost a line for line copy of jung.io.MatrixFile.save, the major
     * difference being that it explicitly uses the US locale to make sure entries
     * use the dot decimal separator (see issue #69) */
    try {
      val writer = new java.io.BufferedWriter(new java.io.FileWriter(filename))
      val matrix = GraphMatrixOperations.graphToSparseMatrix(graph, null) // TODO: provide weights
      for (i <- 0 until matrix.rows) {
        for (j <- 0 until matrix.columns) {
          val w = matrix.getQuick(i, j)
          writer.write("%4.2f ".formatLocal(java.util.Locale.US, w))
        }
        writer.write("\n")
      }
      writer.close()
    } catch {
      case e: Exception => throw new ExtensionException("Error saving file: " + filename, e)
    }
  }

  def load(filename: String, turtleBreed: AgentSet, linkBreed: AgentSet, rng: MersenneTwisterFast) = {
    if (org.nlogo.workspace.AbstractWorkspace.isApplet)
      throw new ExtensionException("Cannot load matrix file when in applet mode.")
    val matrixFile = new jung.io.MatrixFile(
      null, // TODO: provide weight key (null means 1) (issue #19) 
      factoryFor(linkBreed), DummyGraph.vertexFactory, DummyGraph.edgeFactory)
    val graph = matrixFile.load(filename)
    DummyGraph.importToNetLogo(graph, turtleBreed, linkBreed, rng)
  }
}
