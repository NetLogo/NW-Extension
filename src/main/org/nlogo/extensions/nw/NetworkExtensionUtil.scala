// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import scala.collection.JavaConverters._

import org.nlogo.agent
import org.nlogo.agent.TreeAgentSet
import org.nlogo.agent.Turtle
import org.nlogo.api
import org.nlogo.api.Agent
import org.nlogo.api.ExtensionException
import org.nlogo.api.I18N
import org.nlogo.nvm
import org.nlogo.util.MersenneTwisterFast

object NetworkExtensionUtil {

  implicit def functionToTransformer[I, O](f: Function1[I, O]) =
    new org.apache.commons.collections15.Transformer[I, O] {
      override def transform(i: I) = f(i)
    }

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

  implicit def AgentSetToRichAgentSet(agentSet: api.AgentSet) =
    new RichAgentSet(agentSet.asInstanceOf[agent.AgentSet])

  class RichAgentSet(agentSet: agent.AgentSet) {
    lazy val world = agentSet.world.asInstanceOf[org.nlogo.agent.World]
    def isLinkBreed = (agentSet eq world.links) || world.isLinkBreed(agentSet)
    def isTurtleBreed = (agentSet eq world.turtles) || world.isBreed(agentSet)
    def requireTurtleBreed =
      if (isTurtleBreed) agentSet.asInstanceOf[TreeAgentSet]
      else throw new ExtensionException("Expected input to be a turtle breed")
    def requireLinkBreed =
      if (isLinkBreed) agentSet.asInstanceOf[TreeAgentSet]
      else throw new ExtensionException(
        I18N.errors.get("org.nlogo.prim.etc.$common.expectedLastInputToBeLinkBreed"))
    def requireDirectedLinkBreed =
      if (isLinkBreed && agentSet.isDirected) agentSet.asInstanceOf[TreeAgentSet]
      else throw new ExtensionException(
        "Expected input to be a directed link breed")
    def requireUndirectedLinkBreed =
      if (isLinkBreed && !agentSet.isDirected) agentSet.asInstanceOf[TreeAgentSet]
      else throw new ExtensionException(
        "Expected input to be an undirected link breed")

    def asIterable[A]: Iterable[A] = agentSet.agents.asScala.view.map(_.asInstanceOf[A])
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

    // helper function to validate a minimum number of "things" (could be nodes, rows, columns, etc.)
    // and throw an appropriate exception if the value is below that minimum
    protected def getIntValueWithMinimum(arg: api.Argument, minimum: Int, things: String = "nodes") = {
      val nb = arg.getIntValue
      if (nb < minimum)
        throw new ExtensionException(
          "The number of " + things + " in the generated network must be at least " + minimum + ".")
      nb
    }

  }

  def createTurtle(turtleBreed: agent.AgentSet, rng: MersenneTwisterFast) =
    turtleBreed.world.createTurtle(
      turtleBreed,
      rng.nextInt(14), // color
      rng.nextInt(360)) // heading

}

