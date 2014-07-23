package org.nlogo.extensions.nw.algorithms

import org.nlogo.agent.Turtle

trait ClusteringMetrics {
  /**
    * Should return undirected and outgoing links
    **/
  def outNeighbors(turtle: Turtle): Iterable[Turtle]

  def clusteringCoefficient(turtle: Turtle): Double = {
    val neighbors = outNeighbors(turtle)
    val neighborSet = neighbors.toSet
    if (neighbors.size < 2) {
      0
    } else {
      val neighborLinkCounts = neighbors map {
        t: Turtle => (outNeighbors(t) filter { neighborSet.contains _ }).toSeq.length
      }

      neighborLinkCounts.sum.toDouble / (neighbors.size * (neighbors.size - 1))
    }
  }

}
