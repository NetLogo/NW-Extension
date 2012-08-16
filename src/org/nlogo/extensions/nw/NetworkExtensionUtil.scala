package org.nlogo.extensions.nw

import org.nlogo.api
import org.nlogo.agent
import org.nlogo.api.Dump
import org.nlogo.api.Agent
import org.nlogo.api.AgentSet
import org.nlogo.api.Link
import org.nlogo.api.Turtle
import org.nlogo.api.Argument
import org.nlogo.api.ExtensionException
import org.nlogo.api.I18N
import org.nlogo.api.DefaultCommand
import org.nlogo.api.Syntax._
import org.nlogo.api.Context
import org.nlogo.api.TypeNames
import org.nlogo.api.DefaultReporter
import org.nlogo.api.Primitive
import org.nlogo.nvm
import org.nlogo.api.AgentException

object NetworkExtensionUtil {

  implicit def functionToTransformer[I, O](f: Function1[I, O]) =
    new org.apache.commons.collections15.Transformer[I, O] {
      override def transform(i: I) = f(i)
    }

  implicit def AgentSetToNetLogoAgentSet(agentSet: AgentSet) =
    agentSet.asInstanceOf[org.nlogo.agent.AgentSet]
  implicit def AgentToNetLogoAgent(agent: Agent) =
    agent.asInstanceOf[org.nlogo.agent.Agent]
  implicit def TurtleToNetLogoTurtle(turtle: Turtle) =
    turtle.asInstanceOf[org.nlogo.agent.Turtle]
  implicit def LinkToNetLogoLink(link: Link) =
    link.asInstanceOf[org.nlogo.agent.Link]
  implicit def AgentToRichAgent(agent: Agent) = new RichAgent(agent)
  class RichAgent(agent: Agent) {
    def requireAlive =
      if (agent.id != -1) // is alive
        agent
      else throw new ExtensionException(
        I18N.errors.get("org.nlogo.$common.thatAgentIsDead"))
  }

  implicit def LinkToRichLink(link: org.nlogo.agent.Link) = new RichLink(link)
  class RichLink(link: org.nlogo.agent.Link) {
    def getBreedOrLinkVariable(variable: String) =
      try {
        link.world.program.linksOwn.indexOf(variable) match {
          case -1 => link.getLinkBreedVariable(variable)
          case i  => link.getLinkVariable(i)
        }
      } catch {
        case e: Exception => throw new ExtensionException(e)
      }
  }

  implicit def AgentSetToRichAgentSet(agentSet: AgentSet) = new RichAgentSet(agentSet)
  class RichAgentSet(agentSet: AgentSet) {
    def isLinkSet = classOf[Link].isAssignableFrom(agentSet.`type`)
    def isTurtleSet = classOf[Turtle].isAssignableFrom(agentSet.`type`)
    lazy val world = agentSet.world.asInstanceOf[org.nlogo.agent.World]
    def isLinkBreed = (agentSet eq world.links) || world.isLinkBreed(agentSet)
    def isTurtleBreed = (agentSet eq world.turtles) || world.isBreed(agentSet)
    def requireTurtleSet =
      if (isTurtleSet) agentSet
      else throw new ExtensionException("Expected input to be a turtleset")
    def requireTurtleBreed =
      if (isTurtleBreed) agentSet
      else throw new ExtensionException("Expected input to be a turtle breed")
    def requireLinkSet =
      if (isLinkSet) agentSet
      else throw new ExtensionException("Expected input to be a linkset")
    def requireLinkBreed =
      if (isLinkBreed) agentSet
      else throw new ExtensionException(
        I18N.errors.get("org.nlogo.prim.etc.$common.expectedLastInputToBeLinkBreed"))
    def requireDirectedLinkBreed =
      if (isLinkBreed && agentSet.isDirected) agentSet
      else throw new ExtensionException(
        "Expected input to be a directed link breed")
    def requireUndirectedLinkBreed =
      if (isLinkBreed && !agentSet.isDirected) agentSet
      else throw new ExtensionException(
        "Expected input to be an undirected link breed")
  }

  trait turtleCreatingCommand extends api.DefaultCommand with nvm.CustomAssembled {
    // the command itself is observer-only. inside the block is turtle code.
    override def getAgentClassString = "O:-T--"
    def createTurtles(args: Array[api.Argument], context: api.Context): TraversableOnce[agent.Turtle]
    override def perform(args: Array[api.Argument], context: api.Context) {
      val world = context.getAgent.world.asInstanceOf[agent.World]
      val extContext = context.asInstanceOf[nvm.ExtensionContext]
      val nvmContext = extContext.nvmContext
      val turtles = createTurtles(args, context).toArray[agent.Agent]
      turtles.foreach(extContext.workspace.joinForeverButtons)
      val agentSet = new agent.ArrayAgentSet(classOf[agent.Turtle], turtles, world)
      nvmContext.runExclusiveJob(agentSet, nvmContext.ip + 1)
    }
    def assemble(a: nvm.AssemblerAssistant) {
      a.block()
      a.done()
    }

    // helper function to validate a minimum number of turtles:
    protected def getIntValueWithMinimum(arg: api.Argument, minimum: Int, things: String = "nodes") = {
      val nb = arg.getIntValue
      if (nb < minimum)
        throw new ExtensionException(
          "The number of " + things + " in the generated network must be at least " + minimum + ".")
      nb
    }

  }

}

