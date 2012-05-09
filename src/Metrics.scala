package org.nlogo.extensions.nw

import org.nlogo.api.{ LogoList, LogoListBuilder }
import org.nlogo.agent.{ LinkManager, Agent, Turtle, Link, AgentSet, ArrayAgentSet }
import org.nlogo.util.{ MersenneTwisterFast => Random }
import edu.uci.ics.jung.graph.Graph
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath

object Metrics {

  def linkDistanceJung(start: Turtle, end: Turtle, graph: Graph[Turtle, Link]): Option[Int] =
    Option(new DijkstraShortestPath(graph, false).getPath(start, end).size)
      .filterNot(0==)

  def linkPathJung(start: Turtle, end: Turtle, graph: Graph[Turtle, Link]): LogoList = {
    LogoList.fromJava(new DijkstraShortestPath(graph, false).getPath(start, end))
  }

}
