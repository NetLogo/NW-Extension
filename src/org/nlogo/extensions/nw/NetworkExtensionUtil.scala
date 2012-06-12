package org.nlogo.extensions.nw

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
import org.nlogo.nvm.ExtensionContext
import org.nlogo.nvm

object NetworkExtensionUtil {
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
        I18N.errors.get("Expected input to be a directed link breed"))
    def requireUndirectedLinkBreed =
      if (isLinkBreed && !agentSet.isDirected) agentSet
      else throw new ExtensionException(
        I18N.errors.get("Expected input to be an undirected link breed"))
  }
  
  def runCommandTaskForTurtles(turtles: TraversableOnce[Turtle], commandTaskArgument: Argument, context: Context) {
    val command = commandTaskArgument.getCommandTask.asInstanceOf[nvm.CommandTask]
    val emptyArgs = Array[AnyRef]()
    val nvmContext = context.asInstanceOf[ExtensionContext].nvmContext
    for (turtle <- turtles) {
      val newContext = new nvm.Context(nvmContext.job, turtle,
        nvmContext.ip, nvmContext.activation)
      command.perform(newContext, emptyArgs)
    }
  }
}

