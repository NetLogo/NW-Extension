package org.nlogo.extensions.nw.jung

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter

import org.nlogo.agent
import org.nlogo.api

import edu.uci.ics.jung

import org.nlogo.extensions.nw.NetworkExtensionUtil.functionToTransformer

object GraphML {

  def save(graph: jung.graph.Graph[agent.Turtle, agent.Link], filename: String) {

    val graphMLWriter = new jung.io.GraphMLWriter[agent.Turtle, agent.Link]
    val printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename)))

    api.AgentVariables.getImplicitTurtleVariables(false).foreach { v =>
      graphMLWriter.addVertexData(v, null, null,
        (t: agent.Turtle) => api.Dump.logoObject(t.getVariable(v)))
    }

    graphMLWriter.save(graph, printWriter)

  }

}