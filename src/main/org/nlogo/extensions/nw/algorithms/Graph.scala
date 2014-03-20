package org.nlogo.extensions.nw.algorithms

import collection.mutable
import org.nlogo.agent.{Turtle, Link}
import scala.collection.mutable.ArrayBuffer
import org.nlogo.extensions.nw.Memoize

trait Graph {
  private val distances: mutable.Map[(Turtle, Turtle), Int] = mutable.Map()

  private val getPredecessors = Memoize {(p: (Turtle, Turtle)) => ArrayBuffer.empty[Turtle]}
  private val getSuccessors = Memoize {(p: (Turtle, Turtle)) => ArrayBuffer.empty[Turtle]}
  private val getSingleSourceTraversal = Memoize {(source: Turtle) => cachingBFS(source, false, getPredecessors)}
  private val getSingleDestTraversal = Memoize {(dest: Turtle) => cachingBFS(dest, true, getSuccessors)}

  val rng: scala.util.Random
  def neighbors(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean): Iterable[Turtle]

  private var lastSource: Option[Turtle] = None
  private var lastDest: Option[Turtle] = None

  /**
   * Attempts to expand the cache with the least duplicated amount of work possible. It tries to detect users doing
   * single-source and single-destination. Failing that, it expands the caching BFS that is the furthest along, since
   * that one is guaranteed to finish the quickest.
   * @param source
   * @param dest
   */
  private def expandBestTraversal(source: Turtle, dest: Turtle) {
    val sourceTraversal = getSingleSourceTraversal(source)
    val destTraversal = getSingleDestTraversal(dest)
    // If one doesn't have a next, the nodes are disconnected
    if (sourceTraversal.hasNext && destTraversal.hasNext) {
      if (lastSource.exists(_ == source)) {
        sourceTraversal find { _ == dest }
      } else if (lastDest.exists(_ == dest)) {
        destTraversal find { _ == source }
      } else {
        val sourcePosition = sourceTraversal.next()
        val destPosition = destTraversal.next()
        if (sourcePosition != dest && destPosition != source) {
          if (distances((source, sourcePosition)) >= distances((destPosition, dest))) {
            sourceTraversal find { _ == dest }
          }
          if (distances((source, sourcePosition)) <= distances((destPosition, dest))) {
            destTraversal find { _ == source }
          }
        }
      }
    }
    lastSource = Some(source)
    lastDest = Some(dest)
  }

  private def cachedPath(cache: ((Turtle, Turtle)) => Seq[Turtle], source: Turtle, dest: Turtle): Option[List[Turtle]]
    = {
    if (source == dest) {
      Some(List(dest))
    } else {
      val availableSuccessors = cache((source, dest))
      if (availableSuccessors.nonEmpty) {
        val succ = availableSuccessors(rng.nextInt(availableSuccessors.length))
        cachedPath(cache, succ, dest) map {source :: _ }
      } else {
        None
      }
    }
  }

  private def cachedPath(source: Turtle, dest: Turtle): Option[List[Turtle]] =
    cachedPath(getSuccessors, source, dest) orElse cachedPath(getPredecessors, dest, source).map(_.reverse)

  def path(source: Turtle, dest: Turtle): Option[Iterable[Turtle]] = {
    cachedPath(source, dest) orElse {
      expandBestTraversal(source, dest)
      cachedPath(source, dest)
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
  private def cachingBFS(start: Turtle, reverse: Boolean, predecessorCache: ((Turtle,
    Turtle)) => ArrayBuffer[Turtle]): Iterator[Turtle] = {
    val dists = mutable.Map[(Turtle,Turtle), Int]()
    dists((start, start)) = 0

    // note that I can't use the global distances cache to detect visited nodes since
    // the same slot can be filled by either a BFS or reverse BFS.
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
