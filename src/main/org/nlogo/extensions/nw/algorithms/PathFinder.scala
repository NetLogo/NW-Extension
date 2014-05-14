package org.nlogo.extensions.nw.algorithms

import collection.mutable
import org.nlogo.agent.{Agent, World, Turtle, Link}
import scala.collection.mutable.ArrayBuffer
import org.nlogo.extensions.nw.Memoize
import org.nlogo.agent.World.VariableWatcher
import org.nlogo.api.ExtensionException
import scala.ref.WeakReference
import org.nlogo.extensions.nw.util.{Cache, CacheManager}

trait PathFinder {
  private val distanceCaches = CacheManager[(Turtle, Turtle), Double](world)
  private val predecessorCaches = CacheManager(world, (_: Option[String]) => (p: (Turtle, Turtle)) => ArrayBuffer.empty[Turtle])
  private val successorCaches = CacheManager(world, (_: Option[String]) => (p: (Turtle, Turtle)) => ArrayBuffer.empty[Turtle])
  private val singleSourceTraversalCaches = CacheManager[Turtle, Iterator[Turtle]](world, {
    case None => {
      source: Turtle => cachingBFS(source, reverse = false, predecessorCaches(None))
    }
    case Some(varName: String) => {
      source: Turtle =>
        cachingDijkstra(source, weightFunction(varName), reverse = false,
          predecessorCaches(Some(varName)), distanceCaches(Some(varName)))
    }
  }: (Option[String]) => Turtle => Iterator[Turtle])

  def weightFunction(s: String): (Link) => Double

  private val singleDestTraversalCaches = CacheManager[Turtle, Iterator[Turtle]](world, {
    case None => {
      source: Turtle => cachingBFS(source, reverse = true, successorCaches(None))
    }
    case Some(varName: String) => {
      source: Turtle =>
        cachingDijkstra(source, weightFunction(varName), reverse = true,
          successorCaches(Some(varName)), distanceCaches(Some(varName)))
    }
  }: (Option[String]) => Turtle => Iterator[Turtle])

  val rng: scala.util.Random
  def neighbors(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean, shuffle: Boolean = true): Iterable[Turtle]
  def edges(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean, shuffle: Boolean = true): Iterable[Link]

  private var lastSource: Option[Turtle] = None
  private var lastDest: Option[Turtle] = None

  def world: World
  def turtles: Set[Turtle]
  def links: Set[Link]

  /**
   * Attempts to expand the cache with the least duplicated amount of work possible. It tries to detect users doing
   * single-source and single-destination. Failing that, it expands the caching BFS that is the furthest along, since
   * that one is guaranteed to finish the quickest.
   * @param source
   * @param dest
   */
  private def expandBestTraversal(variable: Option[String] = None, source: Turtle, dest: Turtle) {
    val sourceTraversal = singleSourceTraversalCaches(variable)(source)
    val destTraversal = singleDestTraversalCaches(variable)(dest)
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
          if (distanceCaches(variable)((source, sourcePosition)) >= distanceCaches(variable)((destPosition, dest))) {
            sourceTraversal find { _ == dest }
          }
          if (distanceCaches(variable)((source, sourcePosition)) <= distanceCaches(variable)((destPosition, dest))) {
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

  private def cachedPath(variable: Option[String], source: Turtle, dest: Turtle): Option[List[Turtle]] =
    cachedPath(successorCaches(variable), source, dest) orElse cachedPath(predecessorCaches(variable), dest, source).map(_.reverse)

  def path(source: Turtle, dest: Turtle, weightVariable: Option[String] = None): Option[Iterable[Turtle]] = {
    cachedPath(weightVariable, source, dest) orElse {
      expandBestTraversal(weightVariable, source, dest)
      cachedPath(weightVariable, source, dest)
    }
  }

  def distance(source: Turtle, dest: Turtle, weightVariable: Option[String] = None): Option[Double] = {
    distanceCaches(weightVariable).get((source, dest)) orElse {
      expandBestTraversal(weightVariable, source, dest)
      distanceCaches(weightVariable).get(source, dest)
    }
  }

  // TODO: Separate caching and traversing by having traversal return an iterator over (Turtle, Distance, Predecessor)
  // This may be impossible: the caching needs to know about items not actually returned by the traversal (it needs
  // to visit each node once for each predecessor, rather than just once). I tried just having the traversal return
  // nodes for each predecessor but performance was insane. -BCH 4/30/2014
  /*
  This allows us to calculate and store the min spanning tree of start lazily.
  As it traverses the tree, it stores the predecessor and distance information.
  Although the iterator returns one turtle at a time, data about turtles is
  computed a layer at a time so that the cache ends up with complete predecessor
  information for any turtle appearing there. This is crucial or else this class
  will thinks it's done computing paths for a certain pair when it has not.
   */
  private def cachingBFS(start: Turtle, reverse: Boolean, predecessorCache: ((Turtle, Turtle)) => ArrayBuffer[Turtle]): Iterator[Turtle] = {
    val dists = mutable.Map[(Turtle,Turtle), Int]()
    dists((start, start)) = 0

    // note that I can't use the global distances cache to detect visited nodes since
    // the same slot can be filled by either a BFS or reverse BFS.
    val distances = distanceCaches(None)
    distances((start, start)) = 0
    Iterator.iterate(List(start))((last) => {
      var layer: List[Turtle] = List()
      for {
        node <- last
        distance = dists((start, node))
        neighbor <- neighbors(node, includeUn = true, includeIn = reverse, includeOut = !reverse, shuffle = false)
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

  private def cachingDijkstra(start: Turtle, weight: Link => Double, reverse: Boolean, predecessorCache: ((Turtle, Turtle)) => ArrayBuffer[Turtle], distanceCache: Cache[(Turtle, Turtle), Double]): Iterator[Turtle] = {
    val dists = mutable.Map[Turtle, Double]()
    val heap = mutable.PriorityQueue[(Turtle, Double, Turtle)]()(Ordering[Double].on(-_._2))
    distanceCache(start -> start) = 0
    Iterator.continually {
      val curDistance = heap.headOption map { _._2 } getOrElse 0.0
      heap.enqueue((start, 0, start))
      var layer: List[Turtle] = List()
      while (heap.nonEmpty && heap.head._2 <= curDistance) {
        val (turtle, distance, predecessor) = heap.dequeue()
        val alreadyAdded = dists contains turtle
        if (!alreadyAdded || dists(turtle) >= distance) {
          if (!alreadyAdded) {
            layer = turtle :: layer
            dists(turtle) = distance
            if (reverse) {
              distanceCache(turtle -> start) = distance
            } else {
              distanceCache(start -> turtle) = distance
            }
            edges(turtle, includeUn = true, includeIn = reverse, includeOut = !reverse).foreach { link =>
              val other = if (turtle == link.end1 ) link.end2 else link.end1
              val dist = distance + weight(link)
              if (!(dists contains other)) {
                heap.enqueue((other, dist, turtle))
              }
            }
          }
          if (turtle != predecessor) predecessorCache(turtle, start).append(predecessor)

        }
      }
      layer
    }.takeWhile(x => heap.nonEmpty).flatten
  }
}

