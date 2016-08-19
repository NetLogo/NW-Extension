package org.nlogo.extensions.nw

trait Graph[V, E] {

  def otherEnd(node: V)(link: E): V = {
    val (end1, end2) = ends(link)
    if (end2 == node) end1 else end2
  }

  def ends(link: E): (V, V)

  /**
   * Should return both incoming directed and undirected edges.
   */
  def inEdges(node: V): Seq[E]
  /**
   * Should return both incoming directed and undirected neighbors.
   */
  def inNeighbors(node: V): Seq[V] = inEdges(node) map otherEnd(node)

  /**
   * Should return both outgoing directed and undirected edges.
   */
  def outEdges(node: V): Seq[E]
  /**
   * Should return both outgoing directed and undirected neighbors.
   */
  def outNeighbors(node: V): Seq[V] = outEdges(node) map otherEnd(node)

  def allEdges(node: V): Seq[E] = (inEdges(node) ++ outEdges(node)).distinct
  def allNeighbors(node: V): Seq[V] = allEdges(node) map otherEnd(node)

}
