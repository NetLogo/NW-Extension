// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import org.nlogo.agent.AgentSet
import org.nlogo.api.SimpleChangeEventPublisher
import org.nlogo.api.SimpleChangeEvent
import org.nlogo.agent.TreeAgentSet

trait InvalidatableGraph {
  val gc: GraphContext
  val turtleSet: AgentSet = gc.turtleSet // store initial turtle set
  val linkSet: AgentSet = gc.linkSet // store initial link set

  private var _valid = true
  def valid: Boolean = // make sure that
    _valid && // the graph hasn't been invalidated by a monitor
      (turtleSet eq gc.turtleSet) && // the turtle set hasn't changed
      (linkSet eq gc.linkSet) // and the link set hasn't changed

  private var turtleSetChangeEventSub: Option[ChangeSubscriber] =
    changeSubscriber(turtleSet)
  private var linkSetChangeEventSub: Option[ChangeSubscriber] =
    changeSubscriber(linkSet)

  def changeSubscriber(as: AgentSet): Option[ChangeSubscriber] = as match {
    case tas: TreeAgentSet => Some(new ChangeSubscriber(tas))
    case _                 => None
  }

  class ChangeSubscriber(agentSet: TreeAgentSet) extends SimpleChangeEventPublisher#Sub {
    agentSet.simpleChangeEventPublisher.subscribe(this)
    override def notify(pub: SimpleChangeEventPublisher#Pub, event: SimpleChangeEvent) {
      _valid = false
    }
  }

}
