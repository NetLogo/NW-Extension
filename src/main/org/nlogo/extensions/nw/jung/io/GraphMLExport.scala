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
import org.nlogo.extensions.nw.NetworkExtensionUtil.{
  AgentSetToRichAgentSet, functionToTransformer, using
}

object GraphMLExport {

  def save(graphContext: GraphContext, filename: String) = {
    val world = graphContext.world

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

    val turtles = graphContext.nodes
    val links = graphContext.links

    val turtlesOwn = program.turtlesOwn
    val linksOwn = program.linksOwn

    addVariables(
      turtles, turtlesOwn,
      (t: agent.Turtle, v: String) => turtlesOwn.contains(v),
      (t: agent.Turtle, v: String) => t.getVariable(turtlesOwn.indexOf(v)),
      graphMLWriter.addVertexData _)
    addVariables(
      links, linksOwn,
      (t: agent.Link, v: String) => linksOwn.contains(v),
      (t: agent.Link, v: String) => t.getVariable(linksOwn.indexOf(v)),
      graphMLWriter.addEdgeData _)

    addVariables(
      turtles, program.breeds.values.flatMap(_.owns),
      (t: agent.Turtle, v: String) => world.breedOwns(t.getBreed, v),
      (t: agent.Turtle, v: String) => t.getBreedVariable(v),
      graphMLWriter.addVertexData _)
    addVariables(
      links, program.linkBreeds.values.flatMap(_.owns),
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

    try {
      using(new PrintWriter(new BufferedWriter(new FileWriter(filename)))) { printWriter =>
        graphMLWriter.save(graphContext.asJungGraph, printWriter)
      }
    } catch {
      case e: Exception => throw new ExtensionException(e)
    }

  }

  /**
   * Tries to find out the appropriate xml attribute type
   * for a variable by looking at its value for the first agent.
   * NP 2013-08-08
   */
  def findAttrType[T <: agent.Agent](
    agents: Iterable[T],
    varChecker: (T) => Boolean,
    varGetter: (T) => Object) = {
    def attrTypeForValue(x: Any) = x
    (for {
      agent <- agents.headOption
      if varChecker(agent)
    } yield varGetter(agent) match {
      case _: java.lang.Double  => "double"
      case _: java.lang.Boolean => "boolean"
      case _                    => "string"
    }).getOrElse("") // there was no agent; "" will result in no attrType being written
  }
}
