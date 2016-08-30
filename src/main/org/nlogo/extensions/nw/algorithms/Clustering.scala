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
    val initComms = clusterLocally(graph)
    if (initComms.size < graph.nodes.size) {
      val mGraph = MergedGraph(graph, initComms.map(Com(_)))
      val metaComms: Seq[Set[Com[V]]] = cluster(mGraph)
      metaComms.map(_.flatMap(_.members))
    } else {
      initComms
    }
  }


  def clusterLocally[V,E](graph: Graph[V,E]): Seq[Set[V]] = {
    // This way we get the benefits of immutable Set operations while being
    // able to update membership easily.
    var comStruct = CommunityStructure(graph)
    val nodes = graph.nodes.toSeq

    var switchOccurred = true
    while (switchOccurred) {
      switchOccurred  = false
      graph.nodes.foreach { v =>
        // Note that the original community is almost certainly in the connected communities, so we remove it
        val originalCommunity = comStruct.community(v)
        val connectedCommunities = graph.outNeighbors(v).map(comStruct.community _).toSet - originalCommunity
        if (!connectedCommunities.isEmpty) {
          val newComStruct = connectedCommunities.map(comStruct.move(v, _)).maxBy(_.modularity)
          // Require a strictly better score to switch.
          if (newComStruct.modularity > comStruct.modularity) {
            comStruct = newComStruct
            switchOccurred = true
          }
        }
      }
    }
    comStruct.communities
  }

  object CommunityStructure {
    def apply[V,E](graph: Graph[V,E]) = {
      val comMap = graph.nodes.zipWithIndex.toMap
      val internal = Array.fill(graph.nodes.size)(0.0)
      val totalIn = Array.fill(graph.nodes.size)(0.0)
      val totalOut = Array.fill(graph.nodes.size)(0.0)
      graph.nodes.foreach { node =>
        val com = comMap(node)
        graph.outEdges(node).foreach { edge =>
          val weight = graph.weight(edge)
          val other = graph.otherEnd(node)(edge)
          if (com == comMap(other)) internal(com) += weight
          totalOut(com) += weight
          totalIn(comMap(other)) += weight
        }
      }
      val mod = (internal, totalIn, totalOut).zipped.toIterator.map { case (intern: Double, in: Double, out: Double) =>
        (intern - in * out / graph.totalArcWeight) / graph.totalArcWeight
      }.sum
      new CommunityStructure[V,E](graph, comMap, internal.toVector, totalIn.toVector, totalOut.toVector, mod)
    }
  }

  class CommunityStructure[V,E](graph: Graph[V,E], comMap: Map[V, Int], internal: Vector[Double], totalIn: Vector[Double], totalOut: Vector[Double], val modularity: Double) {
    def community(node: V): Int = comMap(node)
    def move(node: V, newCommunity: Int): CommunityStructure[V,E] = {
      val originalCommunity = community(node)
      val otherEnd = graph.otherEnd(node)_
      var inDegree = 0.0
      var outDegree = 0.0
      var internalOriginal = 0.0
      var internalNew = 0.0
      graph.outEdges(node).foreach { link =>
        val other = otherEnd(link)
        val comOther = community(other)
        val weight = graph.weight(link)
        outDegree += weight
        if (other == node) {
          internalOriginal += weight
          internalNew += weight
        } else if (comOther == originalCommunity) {
          internalOriginal += weight
        } else if (comOther == newCommunity) {
          internalNew += weight
        }
      }
      graph.inEdges(node).foreach { link =>
        val other = otherEnd(link)
        val comOther = community(other)
        val weight = graph.weight(link)
        inDegree += weight
        if (other == node) {
          internalOriginal += weight
          internalNew += weight
        } else if (comOther == originalCommunity) {
          internalOriginal += weight
        } else if (comOther == newCommunity) {
          internalNew += weight
        }
      }

      val newTotalIn = totalIn
        .updated(originalCommunity, totalIn(originalCommunity) - inDegree)
        .updated(newCommunity, totalIn(newCommunity) + inDegree)
      val newTotalOut = totalOut
        .updated(originalCommunity, totalOut(originalCommunity) - outDegree)
        .updated(newCommunity, totalOut(newCommunity) + outDegree)
      val newInternal = internal
        .updated(originalCommunity, internal(originalCommunity) - internalOriginal)
        .updated(newCommunity, internal(newCommunity) + internalNew)
      val contrib = (com: Int, intern: Vector[Double], in: Vector[Double], out: Vector[Double]) =>
        (intern(com) - in(com) * out(com) / graph.totalArcWeight) / graph.totalArcWeight

      val deltaOriginal =
        contrib(originalCommunity, newInternal, newTotalIn, newTotalOut) -
        contrib(originalCommunity, internal, totalIn, totalOut)
      val deltaNew =
        contrib(newCommunity, newInternal, newTotalIn, newTotalOut) -
        contrib(newCommunity, internal, totalIn, totalOut)
      new CommunityStructure[V,E](graph, comMap.updated(node, newCommunity), newInternal, newTotalIn, newTotalOut, modularity + deltaOriginal + deltaNew)
    }
    def communities: Seq[Set[V]] = graph.nodes.groupBy(comMap).valuesIterator.map(_.toSet).toList

    // The modularity is actually a pretty good signifier of identity.
    // Just as importantly, its much faster to calculate the hash code of a double
    // then of the graph and the community assignments and so forth.
    override val hashCode = modularity.hashCode

  }

  case class Com[V](members: Set[V]) {
    override val hashCode = members.hashCode
    override def equals(that: Any) = (that.isInstanceOf[AnyRef] && (this eq that.asInstanceOf[AnyRef])) || super.equals(that)
  }

  case class MergedGraph[V,E](graph: Graph[V, E], communities: Seq[Com[V]]) extends Graph[Com[V], (Com[V], Com[V])] {
    val community: Map[V, Com[V]] = communities.flatMap(c => c.members.map(_ -> c))(collection.breakOut)
    override val nodes = communities
    override def ends(link: (Com[V], Com[V])) = link

    val weights: mutable.Map[(Com[V], Com[V]), Double] = mutable.Map.empty[(Com[V], Com[V]), Double]
    graph.nodes.foreach { source =>
      val sourceComm = community(source)
      graph.outEdges(source).foreach { edge =>
        val targetComm = community(graph.otherEnd(source)(edge))
        val commEdge = (sourceComm, targetComm)
        weights(commEdge) = weights.getOrElse(commEdge, 0.0) + graph.weight(edge)
      }
    }

    val inEdgeMap: Map[Com[V], Seq[(Com[V], Com[V])]] = nodes.map { node =>
      node -> node.members.flatMap(v => graph.inNeighbors(v).map(community)).map(_ -> node).toSeq
    }.toMap
    val outEdgeMap: Map[Com[V], Seq[(Com[V], Com[V])]] = nodes.map { node =>
      node -> node.members.flatMap(v => graph.outNeighbors(v).map(community)).map(node -> _).toSeq
    }.toMap


    override def inEdges(node: Com[V]): Seq[(Com[V], Com[V])] = inEdgeMap(node)
    override def outEdges(node: Com[V]): Seq[(Com[V], Com[V])] = outEdgeMap(node)
    override def weight(link: (Com[V], Com[V])): Double = weights(link)
  }
}

