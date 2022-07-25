// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import org.nlogo.agent.Agent
import org.nlogo.agent.AgentSet
import org.nlogo.agent.ArrayAgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.TreeAgentSet
import org.nlogo.agent.Turtle
import org.nlogo.agent.World
import org.nlogo.api.SimpleChangeEvent
import org.nlogo.api.SimpleChangeEventPublisher

class AgentSetChangeSubscriber(agentSet: TreeAgentSet, onNotify: () => Unit)
  extends SimpleChangeEventPublisher#Sub {
  agentSet.simpleChangeEventPublisher.subscribe(this)
  def unsubscribe(): Unit = agentSet.simpleChangeEventPublisher.removeSubscription(this)
  override def notify(pub: SimpleChangeEventPublisher#Pub, event: SimpleChangeEvent.type) {
    onNotify.apply()
  }
}

trait MonitoredAgentSet[A <: Agent] {
  def hasChanged: Boolean
  val agentSet: AgentSet
}

trait MonitoredTreeAgentSet[A <: Agent] extends MonitoredAgentSet[A] {
  override val agentSet: TreeAgentSet
  def world : World
  val breedName = agentSet.printName
  var hasChanged = false
  protected val changeSubscriber = new AgentSetChangeSubscriber(agentSet, () => hasChanged = true)
  def unsubscribe(): Unit = changeSubscriber.unsubscribe()
}

class MonitoredTurtleTreeAgentSet(override val agentSet: TreeAgentSet, val world: World)
  extends MonitoredTreeAgentSet[Turtle]

class MonitoredLinkTreeAgentSet(override val agentSet: TreeAgentSet, val world: World)
  extends MonitoredTreeAgentSet[Link]

trait MonitoredArrayAgentSet[A <: Agent] extends MonitoredAgentSet[A] {
  val agentSet: ArrayAgentSet
  val count = agentSet.count

  def hasChanged: Boolean = count != agentSet.count
}

class MonitoredTurtleArrayAgentSet(override val agentSet: ArrayAgentSet)
  extends MonitoredArrayAgentSet[Turtle]

class MonitoredLinkArrayAgentSet(override val agentSet: ArrayAgentSet)
  extends MonitoredArrayAgentSet[Link]
