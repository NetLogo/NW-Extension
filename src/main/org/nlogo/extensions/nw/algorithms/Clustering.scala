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
    var totalIn: Double = 0
    var totalOut: Double = 0
    var internal: Double = 0
    community.foreach { node =>
      graph.outEdges(node).foreach { edge =>
        val weight = graph.weight(edge)
        if (community contains graph.otherEnd(node)(edge)) internal += weight
        totalOut += weight
      }
      graph.inEdges(node).foreach { edge => totalIn += graph.weight(edge) }
    }
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
        // TODO: Optimize by storing community modularities in parts and updating.
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

  case class MergedGraph[V,E](graph: Graph[V, E], communities: Seq[Set[V]]) extends Graph[Set[V], (Set[V], Set[V])] {
    val community: Map[V, Set[V]] = communities.flatMap(c => c.map(_ -> c))(collection.breakOut)
    override val nodes = communities
    override def ends(link: (Set[V], Set[V])) = link

    val weights: mutable.Map[(Set[V], Set[V]), Double] = mutable.Map.empty[(Set[V], Set[V]), Double]
    graph.nodes.foreach { source =>
      val sourceComm = community(source)
      graph.outEdges(source).foreach { edge =>
        val targetComm = community(graph.otherEnd(source)(edge))
        val commEdge = (sourceComm, targetComm)
        weights(commEdge) = weights.getOrElse(commEdge, 0.0) + graph.weight(edge)
      }
    }

    val inEdgeMap: Map[Set[V], Seq[(Set[V], Set[V])]] = nodes.map{ node =>
      node -> node.flatMap(v => graph.inNeighbors(v).map(community)).map(_ -> node).toSeq
    }.toMap
    val outEdgeMap: Map[Set[V], Seq[(Set[V], Set[V])]] = nodes.map { node =>
      node -> node.flatMap(v => graph.outNeighbors(v).map(community)).map(node -> _).toSeq
    }.toMap


    override def inEdges(node: Set[V]): Seq[(Set[V], Set[V])] = inEdgeMap(node)
    override def outEdges(node: Set[V]): Seq[(Set[V], Set[V])] = outEdgeMap(node)
    override def weight(link: (Set[V], Set[V])): Double = weights(link)
  }
}

