package org.nlogo.extensions.nw.algorithms

import collection.mutable
import org.nlogo.agent.{Turtle, Link}
import scala.collection.mutable.ArrayBuffer

trait Graph {
  private val predecessors: mutable.Map[(Turtle, Turtle), mutable.ArrayBuffer[Turtle]] = mutable.Map()
  private val successors: mutable.Map[(Turtle, Turtle), mutable.ArrayBuffer[Turtle]] = mutable.Map()
  private val distances: mutable.Map[(Turtle, Turtle), Int] = mutable.Map()
  private val singleSourceTraversals: mutable.Map[Turtle, Iterator[Turtle]] = mutable.Map()
  private val singleDestTraversals: mutable.Map[Turtle, Iterator[Turtle]] = mutable.Map()

  val rng: scala.util.Random
  def neighbors(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean): Iterable[Turtle]

  private def getSingleSourceTraversal(source: Turtle): Iterator[Turtle] = {
    singleSourceTraversals.getOrElseUpdate(source, cachingBFS(source, false, getPredecessor))
  }
  private def getSingleDestTraversal(dest: Turtle): Iterator[Turtle] = {
    singleDestTraversals.getOrElseUpdate(dest, cachingBFS(dest, true, getSuccessor))
  }
  private def expandBestTraversal(source: Turtle, dest: Turtle) {
    val sourceTraversal = getSingleSourceTraversal(source)
    val destTraversal = getSingleDestTraversal(dest)
    // If one doesn't have a next, the nodes are disconnected
    if (sourceTraversal.hasNext && destTraversal.hasNext) {
      val sourcePosition = sourceTraversal.next()
      val destPosition = destTraversal.next()
      if (sourcePosition != dest && destPosition != source) {
        if (distances((source, sourcePosition)) > distances((destPosition, dest))) {
          sourceTraversal find { _ == dest }
        } else {
          destTraversal find { _ == source }
        }
      }
    }
  }

  private def getPredecessor(dest: Turtle, source: Turtle): ArrayBuffer[Turtle] = {
    predecessors.getOrElseUpdate((source, dest), ArrayBuffer[Turtle]())
  }
  private def getSuccessor(source: Turtle, dest: Turtle): ArrayBuffer[Turtle] = {
    successors.getOrElseUpdate((source, dest), ArrayBuffer[Turtle]())
  }

  private def cachedPath(cache: (Turtle, Turtle) => Seq[Turtle], source: Turtle, dest: Turtle): Option[List[Turtle]] = {
    if (source == dest) {
      Some(List(dest))
    } else {
      val availableSuccessors = cache(source, dest)
      if (availableSuccessors.nonEmpty) {
        val succ = availableSuccessors(rng.nextInt(availableSuccessors.length))
        cachedPath(cache, succ, dest) map {source :: _ }
      } else {
        None
      }
    }
  }

  def path(source: Turtle, dest: Turtle): Option[Iterable[Turtle]] = {
    cachedPath(getSuccessor, source, dest) orElse cachedPath(getPredecessor, dest, source).map(_.reverse) orElse {
      expandBestTraversal(source, dest)
      cachedPath(getSuccessor, source, dest)
    }
  }

  def distance(source: Turtle, dest: Turtle): Option[Int] = {
    distances.get((source, dest)) orElse {
      expandBestTraversal(source, dest)
      distances.get(source, dest)
    }
  }

  /*
  This allows us to calculate and store the min spanning tree of start lazily.
  As it traverses the tree, it stores the predecessor and distance information.
  Although the iterator returns one turtle at a time, data about turtles is
  computed a layer at a time so that the cache ends up with complete predecessor
  information for any turtle appearing there. This is crucial or else this class
  will thinks it's done computing paths for a certain pair when it has not.
   */
  private def cachingBFS(start: Turtle, reverse: Boolean, predecessorCache: (Turtle,
    Turtle) => ArrayBuffer[Turtle]): Iterator[Turtle] = {
    val dists = mutable.Map[(Turtle,Turtle), Int]()
    dists((start, start)) = 0
    distances((start, start)) = 0
    Iterator.iterate(List(start))((last) => {
      var layer: List[Turtle] = List()
      for {
        node <- last
        distance = dists((start, node))
        neighbor <- neighbors(node, true, reverse, !reverse)
      } {
        if (!dists.contains((start, neighbor))) {
          dists((start, neighbor)) = distance + 1
          if (reverse) {
            distances((neighbor, start)) = distance + 1
          } else {
            distances((start, neighbor)) = distance + 1
          }
          layer = neighbor :: layer
        }
        if (dists((start, neighbor)) == distance + 1) {
          predecessorCache(neighbor, start).append(node)
        }
      }
      layer
    }).takeWhile(_.nonEmpty).flatten
  }
}
