// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jung.io

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter

import scala.collection.JavaConverters._
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable

import org.apache.commons.collections15.Transformer
import org.nlogo.agent
import org.nlogo.api
import org.nlogo.api.ExtensionException
import org.nlogo.extensions.nw.GraphContext
import org.nlogo.extensions.nw.NetworkExtensionUtil.functionToTransformer

object GraphMLExport {

  def save(graphContext: GraphContext, filename: String) {
    val world = graphContext.world

    if (org.nlogo.workspace.AbstractWorkspace.isApplet)
      throw new ExtensionException("Cannot save GraphML file when in applet mode.")

    val graphMLWriter = new GraphMLWriterWithAttribType[agent.Turtle, agent.Link]

    addImplicitVariables(api.AgentVariables.getImplicitTurtleVariables(false), graphMLWriter.addVertexData _)
    addImplicitVariables(api.AgentVariables.getImplicitLinkVariables, graphMLWriter.addEdgeData _)

    def addImplicitVariables[T <: agent.Agent](
      vars: Iterable[String],
      adder: (String, String, String, String, Transformer[T, String]) => Unit) {
      for ((variableName, i) <- vars.zipWithIndex) {
        val transformer = (a: T) => api.Dump.logoObject(a.getVariable(i))
        adder(variableName, null, null, "string", transformer)
      }
    }

    val program = world.program

    val turtlesOwn = program.turtlesOwn.asScala
    val linksOwn = program.linksOwn.asScala

    addVariables(
      graphContext.turtles, turtlesOwn,
      (t: agent.Turtle, v: String) => turtlesOwn.contains(v),
      (t: agent.Turtle, v: String) => t.getVariable(turtlesOwn.indexOf(v)),
      graphMLWriter.addVertexData _)
    addVariables(
      graphContext.links, linksOwn,
      (t: agent.Link, v: String) => linksOwn.contains(v),
      (t: agent.Link, v: String) => t.getVariable(linksOwn.indexOf(v)),
      graphMLWriter.addEdgeData _)

    addVariables(
      graphContext.turtles, program.breedsOwn.asScala.values.flatMap(_.asScala),
      (t: agent.Turtle, v: String) => world.breedOwns(t.getBreed, v),
      (t: agent.Turtle, v: String) => t.getBreedVariable(v),
      graphMLWriter.addVertexData _)
    addVariables(
      graphContext.links, program.linkBreedsOwn.asScala.values.flatMap(_.asScala),
      (l: agent.Link, v: String) => world.linkBreedOwns(l.getBreed, v),
      (l: agent.Link, v: String) => l.getLinkBreedVariable(v),
      graphMLWriter.addEdgeData _)

    def addVariables[T <: agent.Agent](
      agents: Iterable[T],
      vars: Iterable[String],
      varChecker: (T, String) => Boolean,
      varGetter: (T, String) => Object,
      adder: (String, String, String, String, Transformer[T, String]) => Unit) {
      for (variableName <- vars) {
        val attrType = findAttrType(agents,
          varChecker(_: T, variableName),
          varGetter(_: T, variableName))
        val transformer = (a: T) =>
          if (varChecker(a, variableName)) api.Dump.logoObject(varGetter(a, variableName))
          else null
        adder(variableName, null, null, attrType, transformer)
      }
    }

    val printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename)))
    graphMLWriter.save(graphContext.asJungGraph, printWriter)

  }

  /**
   * This is mighty inefficient, but the only way I found to do
   *  that for now, since we are fighting against the dynamic
   *  nature of NetLogo (which may not be wise). It tries to find
   *  out the appropriate xml attribute type for a variable by
   *  looking at its value for the first agent. After that, it
   *  loops through all the agents to check if they are the same.
   *  If anyone differs, it defaults to string. This will be
   *  problematic on very large graphs and with attributes that
   *  may legitimately have mixed types, like colors (i.e.,
   *  Double and LogoList) NP 2013-06-21
   */
  def findAttrType[T <: agent.Agent](
    agents: Iterable[T],
    varChecker: (T) => Boolean,
    varGetter: (T) => Object) = {
    def attrTypeForValue(x: Any) = x match {
      case _: Double  => "double"
      case _: Boolean => "boolean"
      case _          => "string"
    }
    var result: Option[String] = None
    breakable {
      for {
        agent <- agents
        if varChecker(agent)
        at = attrTypeForValue(varGetter(agent))
        if (result match {
          case None =>
            result = Some(at); false
          case Some(at) => false
          case _        => true
        })
      } {
        result = Some("string") // anything mixed results in string
        break
      }
    }
    result.getOrElse("") // there was no agent; "" will result in no attrType being written
  }
}
