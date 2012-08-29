package org.nlogo.extensions.nw.jung

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import org.apache.commons.collections15.Transformer
import org.nlogo.extensions.nw.NetworkExtensionUtil.functionToTransformer
import org.nlogo.agent
import org.nlogo.api
import edu.uci.ics.jung
import org.nlogo.extensions.nw.NetLogoGraph
import org.nlogo.api.ExtensionException
import scala.collection.JavaConverters._

object GraphML {

  def save(graph: NetLogoGraph, filename: String) {

    if (org.nlogo.workspace.AbstractWorkspace.isApplet)
      throw new ExtensionException("Cannot save GraphML file when in applet mode.")

    val graphMLWriter = new jung.io.GraphMLWriter[agent.Turtle, agent.Link]

    addImplicitVariables(api.AgentVariables.getImplicitTurtleVariables(false), graphMLWriter.addVertexData _)
    addImplicitVariables(api.AgentVariables.getImplicitLinkVariables, graphMLWriter.addEdgeData _)

    def addImplicitVariables[T <: agent.Agent](
      vars: Iterable[String],
      adder: (String, String, String, Transformer[T, String]) => Unit) {
      for ((variableName, i) <- vars.zipWithIndex) {
        val transformer = (a: T) => api.Dump.logoObject(a.getVariable(i))
        adder(variableName, null, null, transformer)
      }
    }

    addBreedVariables(
      graph.world.program.breedsOwn.asScala.values.flatMap(_.asScala),
      (t: agent.Turtle, v: String) => graph.world.breedOwns(t.getBreed, v),
      (t: agent.Turtle, v: String) => t.getBreedVariable(v),
      graphMLWriter.addVertexData _)
    addBreedVariables(
      graph.world.program.linkBreedsOwn.asScala.values.flatMap(_.asScala),
      (l: agent.Link, v: String) => graph.world.linkBreedOwns(l.getBreed, v),
      (l: agent.Link, v: String) => l.getLinkBreedVariable(v),
      graphMLWriter.addEdgeData _)

    def addBreedVariables[T <: agent.Agent](
      vars: Iterable[String],
      varChecker: (T, String) => Boolean,
      varGetter: (T, String) => Object,
      adder: (String, String, String, Transformer[T, String]) => Unit) {
      for (variableName <- vars) {
        val transformer = (a: T) =>
          if (varChecker(a, variableName)) api.Dump.logoObject(varGetter(a, variableName))
          else null
        adder(variableName, null, null, transformer)
      }
    }

    val printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename)))
    graphMLWriter.save(graph.asJungGraph, printWriter)

  }

}