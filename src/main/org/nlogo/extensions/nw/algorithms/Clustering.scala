package org.nlogo.extensions.nw.algorithms

import org.nlogo.extensions.nw.Graph
import scala.collection.mutable

object ClusteringMetrics {
  def clusteringCoefficient[V,E](graph: Graph[V,E], node: V): Double = {
    val neighbors = graph.outNeighbors(node)
    val neighborSet = neighbors.toSet
    if (neighbors.size < 2) {
      0
    } else {
      val neighborLinkCounts = neighbors map {
        t: V => (graph.outNeighbors(t) filter { neighborSet.contains _ }).toSeq.length
      }

      neighborLinkCounts.sum.toDouble / (neighbors.size * (neighbors.size - 1))
    }
  }

  def modularity[V,E](graph: Graph[V, E], communities: Iterable[Set[V]]): Double = {
    communities.map(c => communityModularity(graph, c)).sum
  }

  def communityModularity[V,E](graph: Graph[V,E], community: Set[V]): Double = {
    val totalIn: Double = community.view.map(graph.inEdges(_).map(graph.weight _).sum).sum
    val totalOut: Double = community.view.map(graph.outEdges(_).map(graph.weight _).sum).sum
    val internal: Double = community.view.map { v =>
      graph.outEdges(v).filter(e => community contains graph.otherEnd(v)(e)).map(graph.weight _).sum
    }.sum
    (internal - totalIn * totalOut / graph.totalArcWeight) / graph.totalArcWeight
  }

}

object Louvain {
  def cluster[V,E](graph: Graph[V,E]): Seq[Set[V]] = {
    val initComms: Seq[Set[V]] = graph.nodes.map(Set(_)).toSeq
    Iterator.iterate((initComms, MergedGraph(graph, initComms), 1.0)) ({
      (communities: Seq[Set[V]], mGraph: MergedGraph[V,E], modDelta: Double) =>
        val (nextComms: Seq[Set[Set[V]]], nextModDelta: Double) = clusterLocally(mGraph)
        val flattenedComms = nextComms map (_.flatten)
        (flattenedComms, MergedGraph(graph, flattenedComms), nextModDelta)
    }.tupled).dropWhile(_._3 > 0).next._1
  }

  def deltaMod[V,E](graph: Graph[V,E], community: Set[V], node: V): Double =
    // This should also take into account losing the community consisting only of the node,
    // but since all the communities have that term, we can drop it.
    ClusteringMetrics.communityModularity(graph, community + node) - ClusteringMetrics.communityModularity(graph, community)

  def clusterLocally[V,E](graph: Graph[V,E]): (Seq[Set[V]], Double) = {
    // This way we get the benefits of immutable Set operations while being
    // able to update membership easily.
    val nodes = graph.nodes.toSeq
    val members: Array[Set[V]] = nodes.map(Set(_)).toArray
    val communityIndex: mutable.Map[V, Int] = mutable.Map(nodes.zipWithIndex.toSeq: _*)

    val originalMod = ClusteringMetrics.modularity(graph, members)

    var switchOccurred = true
    while (switchOccurred) {
      switchOccurred  = false
      graph.nodes.foreach { v =>
        val originalCommunity = communityIndex(v)
        val originalScore = deltaMod(graph, members(originalCommunity) - v, v)

        // Note that the original community is almost certainly in the connected communities, so we remove it
        val connectedCommunities = graph.outNeighbors(v).map(communityIndex).toSet - originalCommunity
        if (!connectedCommunities.isEmpty) {
          val (best, score) = connectedCommunities.map(i => i -> deltaMod(graph, members(i), v)).maxBy(_._2)
          // Require a strictly better score to switch.
          if (score > originalScore) {
            members(originalCommunity) = members(originalCommunity) - v
            members(best) = members(best) + v
            communityIndex(v) = best
            switchOccurred = true
          }
        }
      }
    }

    val communities: Seq[Set[V]] = members.filterNot(_.isEmpty)
    (communities, ClusteringMetrics.modularity(graph, communities) - originalMod)
  }

  case class MergedGraph[V,E](graph: Graph[V, E], communities: Iterable[Set[V]]) extends Graph[Set[V], (Set[V], Set[V])] {
    val community: Map[V, Set[V]] = communities.flatMap(c => c.map(_ -> c))(collection.breakOut)
    override val nodes = communities.toSet
    override def ends(link: (Set[V], Set[V])) = link

    override def inEdges(node: Set[V]) = node.view.flatMap(v => graph.inNeighbors(v).map(community)).map(_ -> node).toSeq.distinct
    override def outEdges(node: Set[V]) = node.view.flatMap(v => graph.outNeighbors(v).map(community)).map(node -> _).toSeq.distinct
    override def weight(link: (Set[V], Set[V])): Double = link match {
      case (source: Set[V], target: Set[V]) =>
        // Mapping from a set gives a set, which means that we could lose
        // weights that aren't unique. By converting to IterableView, we avoid
        // this.
        source.view.flatMap { v =>
          val edges = graph.outEdges(v).filter(e => community(graph.otherEnd(v)(e)) == target)
          edges.map(graph.weight)
        }.sum
    }
  }
}

