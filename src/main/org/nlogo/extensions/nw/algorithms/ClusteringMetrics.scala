package org.nlogo.extensions.nw.algorithms

trait ClusteringMetrics[V, E] {
  /**
    * Should return undirected and outgoing links
    **/
  def outNeighbors(node: V): Iterable[V]

  def outEdges(node: V): Iterable[E]
  def inEdges(node: V): Iterable[E]

  def otherEnd(node: V)(link: E): V

  def clusteringCoefficient(node: V): Double = {
    val neighbors = outNeighbors(node)
    val neighborSet = neighbors.toSet
    if (neighbors.size < 2) {
      0
    } else {
      val neighborLinkCounts = neighbors map {
        t: V => (outNeighbors(t) filter { neighborSet.contains _ }).toSeq.length
      }

      neighborLinkCounts.sum.toDouble / (neighbors.size * (neighbors.size - 1))
    }
  }

  def modularity(communities: Iterable[Iterable[V]]): Double = {
    val numEnds: Double = communities.map { c =>
      c.map(v => outEdges(v).size).sum
    }.sum
    communities.map { c =>
      val set = c.toSet
      val totalIn = c.map(inEdges(_).size).sum
      val totalOut = c.map(outEdges(_).size).sum
      val internal = c.map(v => outEdges(v).filter(e => set contains otherEnd(v)(e)).size).sum
      internal - totalIn * totalOut /  numEnds
    }.sum / numEnds
  }
}
