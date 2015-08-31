// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import org.nlogo.agent.TreeAgentSet
import org.nlogo.api.{Agent, ExtensionException, I18N}
import org.nlogo.{agent, api, nvm}
import org.nlogo.util.MersenneTwisterFast
import scala.language.{ implicitConversions, reflectiveCalls }

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

    def isLinkSet = classOf[api.Link].isAssignableFrom(agentSet.`type`)
    def isTurtleSet = classOf[api.Turtle].isAssignableFrom(agentSet.`type`)
    def requireTurtleSet =
      if (isTurtleSet) agentSet
      else throw new ExtensionException("Expected input to be a turtleset")
    def requireLinkSet =
      if (isLinkSet) agentSet
      else throw new ExtensionException("Expected input to be a linkset")

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

    private class AgentSetIterable[T <: Agent]
      extends Iterable[T] {
      protected def newIt = agentSet.iterator()
      override def iterator: Iterator[T] = {
        val it = newIt
        new Iterator[T] {
          def hasNext = it.hasNext
          def next() = it.next().asInstanceOf[T]
        }
      }
    }
    def asIterable[T <: Agent]: Iterable[T] = new AgentSetIterable

    private class AgentSetShufflerable[T <: Agent](rng: MersenneTwisterFast)
      extends AgentSetIterable[T] {
      override def newIt = agentSet.shufflerator(rng)
    }
    /** A Shufflerable is to a Shufflerator as an Iterable is to an Iterator */
    def asShufflerable[T <: Agent](rng: MersenneTwisterFast): Iterable[T] =
      new AgentSetShufflerable(rng)

  }

  trait TurtleAskingCommand extends api.DefaultCommand with nvm.CustomAssembled {
    // the command itself is turtle or observer. inside the block is turtle code.
    // Issue #126 provides a good use case for this to be executed in turtle contexts.
    override def getAgentClassString = "OT:-T--"

    def askTurtles(context: api.Context)(turtles: TraversableOnce[agent.Turtle]) = {
      val agents = turtles.toArray[agent.Agent]
      val world = context.getAgent.world.asInstanceOf[agent.World]
      val extContext = context.asInstanceOf[nvm.ExtensionContext]
      val nvmContext = extContext.nvmContext
      agents.foreach(extContext.workspace.joinForeverButtons)
      val agentSet = new agent.ArrayAgentSet(classOf[agent.Turtle], agents, world)
      nvmContext.runExclusiveJob(agentSet, nvmContext.ip + 1)
    }
    def assemble(a: nvm.AssemblerAssistant) {
      a.block()
      a.done()
    }

  }

  trait TurtleCreatingCommand extends TurtleAskingCommand {
    def createTurtles(args: Array[api.Argument], context: api.Context): TraversableOnce[agent.Turtle]
    override def perform(args: Array[api.Argument], context: api.Context) =
      askTurtles(context)(createTurtles(args, context))

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

  def using[A <: { def close() }, B](closeable: A)(body: A => B): B =
    try body(closeable) finally closeable.close()

}

